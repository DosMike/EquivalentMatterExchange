package de.dosmike.sponge.equmatterex;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import de.dosmike.sponge.equmatterex.calculator.Calculator;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class EMCAccount {

    //region Knowledge
    private static final Comparator<ItemTypeEx> ItemTypeValueSorter = (a,b)->{
        Optional<BigInteger> emcA = Calculator.getValueFor(a);
        Optional<BigInteger> emcB = Calculator.getValueFor(b);

        return (!emcA.isPresent() && !emcB.isPresent()) ? 0
            : (!emcA.isPresent() ? -1
                : (!emcB.isPresent() ? 1
                    : -(emcA.get().compareTo(emcB.get()))
                )
            );

    };

    /** storing all items a user has put into the emc table at some point */
    private static Map<UUID, List<ItemTypeEx>> knowledge = new LinkedHashMap<>();
    private static final Object knowledgeMutex = new Object();
    public static boolean learn(Player player, ItemTypeEx item) {
        assert !item.equals(ItemTypes.AIR) : "Can't learn air";
        synchronized (knowledgeMutex) {
            List<ItemTypeEx> known = knowledge.getOrDefault(player.getUniqueId(), new LinkedList<>());
            if (!known.contains(item)) {
                known.add(item);
                known.sort(ItemTypeValueSorter);
                knowledge.put(player.getUniqueId(), known);
                return true;
            } else {
                return false;
            }
        }
    }
//    private static boolean contains (Collection<ItemType> set, ItemType one) {
//        for (ItemType item : set) {
//            if (item.getId().equals(one.getId()))
//                return true;
//        }
//        return false;
//    }
    public static List<ItemTypeEx> getKnowledgePage(Player player, int page, int pagesize, BigInteger maxValue) {
        assert page>0 : "Page can't be less than 1";
        assert pagesize>0 : "Pagesize can't be less than 1";
        synchronized (knowledgeMutex) {
            List<ItemTypeEx> known = knowledge.getOrDefault(player.getUniqueId(), new LinkedList<>());
            return known.stream()
                    .filter(item->{ //filter affordable values
                        Optional<BigInteger> val = Calculator.getValueFor(item);
                        return val.isPresent() && val.get().compareTo(maxValue)<=0;
                    })
                    .skip((page-1)*(long)pagesize)
                    .limit(pagesize)
                    .collect(Collectors.toList());
        }
    }
    public static ImmutableCollection<ItemTypeEx> getAllKnowledge(Player player) {
        synchronized (knowledgeMutex) {
            return ImmutableSet.<ItemTypeEx>builder()
                    .addAll(knowledge.getOrDefault(player.getUniqueId(), new LinkedList<>()))
                    .build();
        }
    }

    static void unloadKnowledge(Player player) {
        synchronized (knowledgeMutex) {
            knowledge.remove(player.getUniqueId());
        }
    }
    static void loadKnowledge(Player player, Collection<ItemTypeEx> toKnow) {
        synchronized (knowledgeMutex) {
            List<ItemTypeEx> known = knowledge.getOrDefault(player.getUniqueId(), new LinkedList<>());
            for (ItemTypeEx item : toKnow) {
                if (!known.contains(item))
                    known.add(item);
            }
            known.sort(ItemTypeValueSorter);
            knowledge.put(player.getUniqueId(), known);
        }
    }
    //endregion

    //region EMC account
    private static Map<UUID, BigInteger> bank = new HashMap<>();
    private static final Object bankMutex = new Object();

    public static BigInteger getBalance(Player player) {
        synchronized (bankMutex) {
            return bank.getOrDefault(player.getUniqueId(), BigInteger.ZERO);
        }
    }

    /** @return the new balance */
    public static BigInteger deposit(Player player, BigInteger amount) {
        if (amount.compareTo(BigInteger.ZERO)<0)
            throw new IllegalArgumentException("Can't deposit negative amount");
        synchronized (bankMutex) {
            BigInteger balance = bank.getOrDefault(player.getUniqueId(), BigInteger.ZERO).add(amount);
            bank.put(player.getUniqueId(), balance);
            return balance;
        }
    }

    /** @return the amount of actually withdrawn money */
    public static BigInteger withdraw(Player player, BigInteger amount) {
        if (amount.compareTo(BigInteger.ZERO)<0)
            throw new IllegalArgumentException("Can't withdraw negative amount");
        synchronized (bankMutex) {
            BigInteger balance = bank.getOrDefault(player.getUniqueId(), BigInteger.ZERO);
            if (balance.compareTo(amount)>=0) {
                balance = balance.subtract(amount);
                bank.put(player.getUniqueId(), balance);
                return amount;
            } else {
                bank.put(player.getUniqueId(), BigInteger.ZERO);
                return balance;
            }
        }
    }

    /** @return true if enough EMC is available */
    public static boolean hasBalance(Player player, BigInteger amount) {
        if (amount.compareTo(BigInteger.ZERO)<0)
            throw new IllegalArgumentException("Can't check for negative amount");
        synchronized (bankMutex) {
            return (bank.getOrDefault(player.getUniqueId(), BigInteger.ZERO).compareTo(amount)>=0);
        }
    }

    static void loadAccount(Player player, BigInteger balance) {
        synchronized (bankMutex) {
            bank.put(player.getUniqueId(), balance);
        }
    }
    static BigInteger unloadAccount(Player player) {
        synchronized (bankMutex) {
            return (bank.containsKey(player.getUniqueId()))
                    ? bank.remove(player.getUniqueId())
                    : BigInteger.ZERO;
        }
    }
    //endregion

    private static final JsonParser jparser = new JsonParser();
    private static final Gson gson = new Gson();

    public static void loadFromFile(Player player) {
        File f = EquivalentMatter.getInstance().getConfigDir().resolve("player").toFile();
        f.mkdirs();
        f = new File(f, player.getUniqueId().toString());
        if (!f.exists()) return;

        JsonObject json = new JsonObject();

        try {
            json = jparser.parse(new InputStreamReader(new FileInputStream(f))).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadAccount(player, json.get("balance").getAsBigInteger());
        List<ItemTypeEx> items = new LinkedList<>();
        json.get("knowledge").getAsJsonArray().forEach(item->
                ItemTypeEx.valueOf(item.getAsString())
                        .ifPresent(items::add)
        );
        loadKnowledge(player, items);
    }

    public static void saveToFile(Player player) {
        int ok=2;
        synchronized (bankMutex) {
            if (bank.containsKey(player.getUniqueId())) ok--;
        }
        synchronized (knowledgeMutex) {
            if (knowledge.containsKey(player.getUniqueId())) ok--;
        }
        if (ok==2) return; //no data to safe

        File f = EquivalentMatter.getInstance().getConfigDir().resolve("player").toFile();
        f.mkdirs();
        f = new File(f, player.getUniqueId().toString());

        JsonObject json = new JsonObject();
        json.addProperty("balance", unloadAccount(player).toString());
        JsonArray array = new JsonArray();
        getAllKnowledge(player).forEach(item->array.add(item.getId()));
        unloadKnowledge(player);
        json.add("knowledge", array);

        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(f)));
            gson.toJson(json, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
