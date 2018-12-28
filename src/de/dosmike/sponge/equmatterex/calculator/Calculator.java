package de.dosmike.sponge.equmatterex.calculator;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.ItemTypeEx;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.smelting.SmeltingRecipe;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Calculator {

    private static Map<ItemTypeEx, BigInteger> values = new HashMap<>();
    private static Set<ItemTypeEx> fixedCost = new HashSet<>();
    static int verbosity = 1;
    static final int logStillWorkingNotificationDelay = 15_000;
    public static void setVerbosity(int level) {
        if (level < 0 || level > 3)
            throw new IllegalArgumentException("Illegal verbosity level");
        verbosity = level;
    }

    private static List<WorldConversion> conversions = new LinkedList<>();
    public static void setConversion(WorldConversion conversion) {
        conversions.add(conversion);
    }

    /** calling this requires recalculation */
    public static void resetAndDefaults() {
        values.clear();
        fixedCost.clear();
        conversions.clear();

        //region Defaults
        setFixCost(ItemTypeEx.of(ItemTypes.COBBLESTONE, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONE, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONE, 1), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONE, 3), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONE, 5), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONEBRICK, 2), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.STONEBRICK, 3), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.END_STONE, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.NETHERRACK, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.DIRT, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.SAND, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.SAND, 1), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.SNOW, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.ICE, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.DEADBUSH, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.GRAVEL, 0), 4);
        setFixCost(ItemTypeEx.of(ItemTypes.CACTUS, 0), 8);
        setFixCost(ItemTypeEx.of(ItemTypes.VINE, 0), 8);
        setFixCost(ItemTypeEx.of(ItemTypes.WEB, 0), 12);
        setFixCost(ItemTypeEx.of(ItemTypes.WATERLILY, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 1), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 2), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 3), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 4), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 5), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 6), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 7), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_FLOWER, 8), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 1), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 4), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 5), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.YELLOW_FLOWER, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RED_MUSHROOM, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.BROWN_MUSHROOM, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.REEDS, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.SOUL_SAND, 0), 49);
        setFixCost(ItemTypeEx.of(ItemTypes.OBSIDIAN, 0), 64);
        setFixCost(ItemTypes.STAINED_HARDENED_CLAY, 64);
        setFixCost(ItemTypeEx.of(ItemTypes.SPONGE, 0), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.SPONGE, 1), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.TALLGRASS, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.TALLGRASS, 1), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.TALLGRASS, 2), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 2), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.DOUBLE_PLANT, 3), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.PACKED_ICE, 0), 4);
        setFixCost(ItemTypeEx.of(ItemTypes.MAGMA, 0), 128);

        setFixCost(ItemTypeEx.of(ItemTypes.CHORUS_PLANT, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.CHORUS_FLOWER, 0), 96);
        setFixCost(ItemTypeEx.of(ItemTypes.WHEAT_SEEDS, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.BEETROOT_SEEDS, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.MELON, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.WHEAT, 0), 24);
        setFixCost(ItemTypeEx.of(ItemTypes.NETHER_WART, 0), 24);
        setFixCost(ItemTypeEx.of(ItemTypes.APPLE, 0), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.PUMPKIN, 0), 144);
        setFixCost(ItemTypeEx.of(ItemTypes.PORKCHOP, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.BEEF, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.CHICKEN, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.RABBIT, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.MUTTON, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.FISH, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.FISH, 1), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.FISH, 2), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.FISH, 3), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.CARROT, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.BEETROOT, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.POTATO, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.POISONOUS_POTATO, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.CHORUS_FRUIT, 0), 192);

        setFixCost(ItemTypeEx.of(ItemTypes.STRING, 0), 12);
        setFixCost(ItemTypeEx.of(ItemTypes.ROTTEN_FLESH, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.SLIME_BALL, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.EGG, 0), 32);
        setFixCost(ItemTypeEx.of(ItemTypes.FEATHER, 0), 48);
        setFixCost(ItemTypeEx.of(ItemTypes.RABBIT_HIDE, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.RABBIT_FOOT, 0), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.SPIDER_EYE, 0), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.GUNPOWDER, 0), 192);
        setFixCost(ItemTypeEx.of(ItemTypes.ENDER_PEARL, 0), 1024);
        setFixCost(ItemTypeEx.of(ItemTypes.BLAZE_ROD, 0), 1536);
        setFixCost(ItemTypeEx.of(ItemTypes.SHULKER_SHELL, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.GHAST_TEAR, 0), 4096);
        setFixCost(ItemTypeEx.of(ItemTypes.DRAGON_EGG, 0), 262144);
        setFixCost(ItemTypeEx.of(ItemTypes.POTION, 0), 0);
        setFixCost(ItemTypeEx.of(ItemTypes.PURPLE_SHULKER_BOX, 0), 0);

        setFixCost(ItemTypeEx.of(ItemTypes.SADDLE, 0), 192);
        setFixCost(ItemTypeEx.of(ItemTypes.NAME_TAG, 0), 192);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_11, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_13, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_BLOCKS, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_CAT, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_CHIRP, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_FAR, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_MALL, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_MELLOHI, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_STAL, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_STRAD, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_WAIT, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.RECORD_WARD, 0), 2048);

        setFixCost(ItemTypeEx.of(ItemTypes.IRON_INGOT, 0), 256);
        setFixCost(ItemTypeEx.of(ItemTypes.GOLD_INGOT, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.DIAMOND, 0), 8192);
        setFixCost(ItemTypeEx.of(ItemTypes.FLINT, 0), 4);
        setFixCost(ItemTypeEx.of(ItemTypes.COAL, 0), 128);
        setFixCost(ItemTypeEx.of(ItemTypes.REDSTONE, 0), 64);
        setFixCost(ItemTypeEx.of(ItemTypes.GLOWSTONE_DUST, 0), 384);
        setFixCost(ItemTypeEx.of(ItemTypes.QUARTZ, 0), 256);
        setFixCost(ItemTypeEx.of(ItemTypes.PRISMARINE_SHARD, 0), 256);
        setFixCost(ItemTypeEx.of(ItemTypes.PRISMARINE_CRYSTALS, 0), 512);
        setFixCost(ItemTypeEx.of(ItemTypes.DYE, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.DYE, 3), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.DYE, 4), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.ENCHANTED_BOOK, 0), 2048);
        setFixCost(ItemTypeEx.of(ItemTypes.EMERALD, 0), 16384);
        setFixCost(ItemTypeEx.of(ItemTypes.NETHER_STAR, 0), 139264);
        setFixCost(ItemTypeEx.of(ItemTypes.CLAY_BALL, 0), 16);
        setFixCost(ItemTypeEx.of(ItemTypes.BONE, 0), 144);
        setFixCost(ItemTypeEx.of(ItemTypes.SNOWBALL, 0), 1);
        setFixCost(ItemTypeEx.of(ItemTypes.FILLED_MAP, 0), 1472);

        setFixCost(ItemTypes.LOG, 32);
        setFixCost(ItemTypes.LOG2, 32);
        setFixCost(ItemTypes.SAPLING, 32);
        setFixCost(ItemTypes.PLANKS, 8);
        setFixCost(ItemTypeEx.of(ItemTypes.STICK,0), 4);
        setFixCost(ItemTypes.LEAVES, 1);
        setFixCost(ItemTypes.LEAVES2, 1);

        setFixCost(ItemTypeEx.of(ItemTypes.GLASS,0), 1);
        setFixCost(ItemTypes.STAINED_GLASS, 1);
        setFixCost(ItemTypes.WOOL, 48);

        setFixCost(ItemTypes.STONE, 1);
        //endRegion
        //region Conversions
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.GRASS, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.DIRT, 2).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.MYCELIUM, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.DIRT, 2).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.GRASS_PATH, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.GRASS, 1).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.IRON_HORSE_ARMOR, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.IRON_INGOT, 8).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.GOLDEN_HORSE_ARMOR, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.GOLD_INGOT, 8).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.DIAMOND_HORSE_ARMOR, 1).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.DIAMOND, 8).createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 0).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 0).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 1).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER,1).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 2).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 2).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 3).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 3).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 4).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 4).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 5).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 5).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 6).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 6).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 7).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 7).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 8).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 8).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 9).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 9).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 10).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 10).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 11).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 11).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 12).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 12).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 13).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 13).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 14).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 14).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.CONCRETE, 15).itemStack().createSnapshot(),
                Collections.singletonList(ItemTypeEx.of(ItemTypes.CONCRETE_POWDER, 15).itemStack().createSnapshot())));
        setConversion(new WorldConversion(ItemTypeEx.of(ItemTypes.GOLDEN_APPLE, 1).itemStack().createSnapshot(),
                Arrays.asList(ItemStack.of(ItemTypes.APPLE, 1).createSnapshot(),
                                ItemStack.of(ItemTypes.GOLD_BLOCK, 8).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.WATER_BUCKET).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.BUCKET).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.MILK_BUCKET).createSnapshot(),
                Collections.singletonList(ItemStack.of(ItemTypes.BUCKET).createSnapshot())));
        setConversion(new WorldConversion(ItemStack.of(ItemTypes.LAVA_BUCKET).createSnapshot(),
                Arrays.asList(ItemStack.of(ItemTypes.BUCKET).createSnapshot(),
                                ItemStack.of(ItemTypes.OBSIDIAN).createSnapshot())));
        //endRegion

        try {
            ConfigurationLoader<CommentedConfigurationNode> defaultCfg = HoconConfigurationLoader.builder()
                    .setPath(EquivalentMatter.getInstance().getConfigDir().resolve("defaultValues.conf"))
                    .build();

            CommentedConfigurationNode root = defaultCfg.createEmptyNode();
            root.setComment("This configuration lists the internal default values and is meant as reference!\nChanging this configuration has no effect, as it will be rewritten every time the values regenerate");
            Map<String, String> values = new HashMap<>();
            Calculator.getAllValues().forEach((key, value) -> values.put(key.getId(), value.toString()));
            root.getNode("presets").setValue(new TypeToken<Map<String, String>>(){}, values);
            ConfigurationNode cfgCon = root.getNode("worldconversions");
            for (WorldConversion con : conversions) {
                ConfigurationNode entry = cfgCon.getAppendedNode();
                entry.getNode("output").setValue(ItemTypeEx.of(con.getExemplaryResult()).getId());
                ConfigurationNode cfgIng = entry.getNode("ingredients");
                for (ItemStackSnapshot i : con.getRequiredInputs()) {
                    cfgIng.getNode(ItemTypeEx.of(i).getId()).setValue(i.getQuantity());
                }
            }
            defaultCfg.save(root);
        } catch (ObjectMappingException | IOException e) {
            EquivalentMatter.w("Failed to write defaults");
            e.printStackTrace();
        }
    }
    static Map<ItemTypeEx, BigInteger> getAllValues() {
        return values;
    }

    private static void safeValues() {
        try {
            ConfigurationLoader<CommentedConfigurationNode> defaultCfg = HoconConfigurationLoader.builder()
                    .setPath(EquivalentMatter.getInstance().getConfigDir().resolve("emcValues.conf"))
                    .build();

            CommentedConfigurationNode root = defaultCfg.createEmptyNode();
            root.setComment("I recommend you change values in-game and regenerate them");
            Map<String, String> presets = new HashMap<>();
            Map<String, String> calculated = new HashMap<>();
            Calculator.getAllValues().forEach((key, value) -> (fixedCost.contains(key)?presets:calculated).put(key.getId(), value.toString()));
            root.getNode("presets").setValue(new TypeToken<Map<String, String>>(){}, presets);
            ConfigurationNode cfgCon = root.getNode("worldconversions");
            for (WorldConversion con : conversions) {
                ConfigurationNode entry = cfgCon.getAppendedNode();
                entry.getNode("output").setValue(ItemTypeEx.of(con.getExemplaryResult()).getId());
                ConfigurationNode cfgIng = entry.getNode("ingredients");
                for (ItemStackSnapshot i : con.getRequiredInputs()) {
                    cfgIng.getNode(ItemTypeEx.of(i).getId()).setValue(i.getQuantity());
                }
            }
            root.getNode("calculated").setValue(new TypeToken<Map<String, String>>(){}, calculated);
            CommentedConfigurationNode verb = root.getNode("calulatorVerbosity");
            verb.setComment("This value defines, how much the console gets spammed when regenerating EMC. Values reach from 0 (pretty silent) to 3 (almost everything).");
            verb.setValue(verbosity);
            defaultCfg.save(root);
        } catch (ObjectMappingException|IOException e) {
            EquivalentMatter.w("Failed to write values");
            e.printStackTrace();
        }
    }

    /** calling this requires recalculation */
    public static void setFixCost(ItemTypeEx item, BigInteger cost) {
        fixedCost.add(item);
        values.put(item, cost);
    }

    /** calling this requires recalculation */
    public static void setFixCost(ItemTypeEx item, int cost) {
        setFixCost(item, BigInteger.valueOf(cost));
    }

    /** sets the cost for all N variants in this type */
    public static void setFixCost(ItemType type, int cost) {
        int max=16;
        if (type.equals(ItemTypes.LOG) ||
            type.equals(ItemTypes.LEAVES)) {
            max = 4;
        } else if(type.equals(ItemTypes.LOG2) ||
                type.equals(ItemTypes.LEAVES2)) {
            max = 2;
        } else if(type.equals(ItemTypes.PLANKS) ||
                type.equals(ItemTypes.SAPLING)) {
            max = 6;
        } else if (type.equals(ItemTypes.STAINED_GLASS) ||
                type.equals(ItemTypes.WOOL)||
                type.equals(ItemTypes.STAINED_HARDENED_CLAY)
                ) {
            max = 16;
        } else if (type.equals(ItemTypes.STONE)) {
            max = 7;
        }

        for (int i = 0; i < max; i++)
            setFixCost(ItemTypeEx.of(type, i), BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setFixCost(ItemTypeEx item, long cost) {
        setFixCost(item, BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemTypeEx item, BigInteger cost) {
        values.put(item, cost);
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemTypeEx item, int cost) {
        setTemporaryCost(item, BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemTypeEx item, long cost) {
        setTemporaryCost(item, BigInteger.valueOf(cost));
    }

    /** removes fixed status and allows the value to be calculated again */
    public static boolean resetCost(ItemTypeEx item) {
        return fixedCost.remove(item);
    }

    private static AtomicBoolean calculating = new AtomicBoolean(false);
    public static boolean isCalculating() {
        return calculating.get();
    }
    /** break glass in case of exception */
    public static void resetState() {
        calculating.set(false);
    }
    public static void calculate() {
        if (calculating.getAndSet(true))
            throw new IllegalStateException("EMC values are already rebuilding");

        int changes = 1, maxIterations = 25;

        for (int iteration = 0; iteration < maxIterations && changes > 0; iteration++) {
            if (verbosity>0) EquivalentMatter.w("Iteration %d/%d:", iteration+1, maxIterations);
            changes = 0;
            int scanned;
            //region Crafting
            long t0 = System.currentTimeMillis();
            {
                Map<ItemTypeEx, BigInteger> newValues = new HashMap<>();
                MergeFunction<BigInteger> MERGE = BigInteger::min;
                List<CraftingRecipe> recipes = new LinkedList<>(Sponge.getRegistry().getCraftingRecipeRegistry().getRecipes());
                int spliteration = Runtime.getRuntime().availableProcessors() * 4;
                EquivalentMatter.l("Crafting Recipe Resolution over %d threads", spliteration);
                spliteration = Math.min(spliteration, recipes.size()/4); // in case there'd be more than (recipe/4) cores on the system
                int spliterationSize = recipes.size()/spliteration;
                List<Future<Map<ItemTypeEx, BigInteger>>> futures = new LinkedList<>();
                for (int i = 1; i <= spliteration; i++) {
                    List<CraftingRecipe> subList = new LinkedList<>();
                    for (int z = (i-1)*spliterationSize; z < ((i<spliteration)?(i*spliterationSize):recipes.size()); z++)
                        subList.add(recipes.get(z));
                    EquivalentMatter.l("Starting thread for %s", String.join(", ", subList.stream().map(r->r.getExemplaryResult().getTranslation().get()).collect(Collectors.toList())));
                    futures.add(EquivalentMatter.getAsyncExecutor()
                            .submit(new CraftingRecipeMapper(subList)));
                }
                try {
                    for (Future<Map<ItemTypeEx, BigInteger>> mapFuture : futures) {
                        newValues = MERGE.mergeMaps(newValues, mapFuture.get());
                    }
                } catch (Exception e) {/**/}
                changes += newValues.size();
                if (verbosity>0) EquivalentMatter.w("Crafting scan %.2fs for %d recipes", (double) (System.currentTimeMillis()-t0)/1000.0, recipes.size());
                values = MERGE.mergeMaps(values, newValues);
            }
            //endregion
            //region Smelting
            t0 = System.currentTimeMillis();
            scanned = 0;
            EquivalentMatter.l("Smelting Recipe Resolution");
            {
                Collection<SmeltingRecipe> smeltingRecipes = Sponge.getGame().getRegistry().getSmeltingRecipeRegistry().getRecipes();
                if (values.containsKey(ItemTypeEx.of(ItemTypes.COAL))) {//min required to calculate
                    //one coal block can smelt 8 items
                    BigInteger costPerSmelt = values.get(ItemTypeEx.of(ItemTypes.COAL)).divide(BigInteger.valueOf(8));
                    for (SmeltingRecipe recipe : smeltingRecipes) {
                        if (verbosity>2) EquivalentMatter.l("Checking Smelting:\n%s\n => %s x%d", ItemTypeEx.of(recipe.getExemplaryIngredient()).getId(),  ItemTypeEx.of(recipe.getExemplaryResult()).getId(), recipe.getExemplaryResult().getQuantity());
                        ItemTypeEx input = ItemTypeEx.of(recipe.getExemplaryIngredient());
                        BigInteger cost = values.get(input);
                        ItemTypeEx output = ItemTypeEx.of(recipe.getExemplaryResult());
                        if (output.getType().equals(ItemTypes.GOLD_NUGGET) || output.getType().equals(ItemTypes.IRON_NUGGET))
                            continue; //these will cause a decremental loop, eventually decreasing the value for ingots to 1
                        BigInteger previous = values.get(output);
                        if (cost == null) {
                            if (/*input can't be fixed cost if null */ previous != null && previous.compareTo(BigInteger.ZERO)>0) {
                                cost = previous.subtract(costPerSmelt);
                                values.put(input, cost);
                                if (verbosity > 0)
                                    EquivalentMatter.w("Reversed value for %s from %s to be %s EMC", input.getId(), output.getId(), cost.toString());
                            }
                        } else {
                            cost = cost.add(costPerSmelt);
                            if (!fixedCost.contains(output)) {
                                if (previous == null || previous.compareTo(cost) > 0) {
                                    values.put(output, cost);
                                    if (verbosity > 0)
                                        EquivalentMatter.w("Updated %s to %s EMC", output.getId(), cost.toString());
                                    changes++;
                                }
                            }
                        }
                        if (System.currentTimeMillis() - t0 > logStillWorkingNotificationDelay) {
                            if (verbosity > 0)
                                EquivalentMatter.w("Smelting scan %.2f%% (%d/%d) done...", (double) scanned * 100.0 / smeltingRecipes.size(), scanned, smeltingRecipes.size());
                            t0 = System.currentTimeMillis();
                        }
                        scanned++;
                    }
                } else {
                    EquivalentMatter.w("No cost for coal determined");
                }
            }
            //endregion
            //region WorldConversions
            t0 = System.currentTimeMillis();
            scanned = 0;
            EquivalentMatter.l("World Conversion Recipe Resolution");
            {
                for (WorldConversion recipe : conversions) {
                    if (verbosity>2) EquivalentMatter.l("Checking World Conversion %s to %dx %s", String.join(", ",
                            recipe.getRequiredInputs().stream().map(e->e.getQuantity()+"x "+ItemTypeEx.of(e).getId()).collect(Collectors.toList())
                            ), recipe.getExemplaryResult().getQuantity(), ItemTypeEx.of(recipe.getExemplaryResult()).getId());
                    BigInteger cost = BigInteger.ZERO;
                    boolean missing = false;
                    for (ItemStackSnapshot input : recipe.in) {
                        if (!values.containsKey(ItemTypeEx.of(input))) {
                            missing = true;
                            break;
                        }
                        cost = cost.add(values.get(ItemTypeEx.of(input)).multiply(BigInteger.valueOf(input.getQuantity())));
                    }
                    ItemTypeEx output = ItemTypeEx.of(recipe.getExemplaryResult());
                    //cost = cost.divide(BigInteger.valueOf(recipe.getExemplaryResult().getQuantity()));
                    if (missing || cost.compareTo(BigInteger.ZERO)<=0) continue;
                    if (!fixedCost.contains(output)) {
                        BigInteger previous = values.get(output);
                        if (previous == null || previous.compareTo(cost) > 0) {
                            values.put(output, cost);
                            if (verbosity > 0)
                                EquivalentMatter.w("Updated %s to %s EMC", output.getId(), cost.toString());
                            changes++;
                        }
                    }
                    if (System.currentTimeMillis() - t0 > logStillWorkingNotificationDelay) {
                        if (verbosity > 0)
                            EquivalentMatter.w("Conversion scan %.2f%% (%d/%d) done...", (double) scanned * 100.0 / conversions.size(), scanned, conversions.size());
                        t0 = System.currentTimeMillis();
                    }
                    scanned++;
                }
            }
            //endregion
            EquivalentMatter.l("[Calculator] Updated %d EMC values", changes);
        }
        if (changes > 0) {
            EquivalentMatter.w("[Calculator] Could not resolve all costs after %d iterations!", maxIterations);
        }
        Collection<String> missing = Sponge.getRegistry()
                .getAllOf(ItemType.class)
                .stream()
                .filter(it->!values.containsKey(ItemTypeEx.of(it)))
                .map(ItemType::getName)
                .collect(Collectors.toList());
        if (missing.size()>0) {
            EquivalentMatter.w("[Calculator] The following items are still missing: %s ", String.join(", ", missing));
        }

        EquivalentMatter.l("Saving calculated values...");
        safeValues();

        calculating.set(false);
    }

    /** returns a value only if it's present and above 0.
     * this can easily be wrapped in optionals.
     * this automatically removes any item that was not set or manually nulled
     * while containsValue still returns whether a value was set manually */
    private static BigInteger getAboveZero(ItemTypeEx type) {
        BigInteger val = values.get(type);
        if (val != null && val.compareTo(BigInteger.ZERO)<=0)
            return null;
        else
            return val;
    }
    public static Optional<BigInteger> getValueFor(ItemTypeEx type) {
        return Optional.ofNullable(getAboveZero(type));
    }

    public static Optional<BigInteger> getValueFor(ItemStack stack) {
        return Optional.ofNullable(getAboveZero(ItemTypeEx.of(stack)))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())));
    }

    public static Optional<BigInteger> getValueFor(ItemStackSnapshot stack) {
        return Optional.ofNullable(getAboveZero(ItemTypeEx.of(stack)))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())));
    }
    static BigInteger getValueRaw(ItemTypeEx type) {
        return values.get(type);
    }

    static BigInteger getValueRaw(ItemStack stack) {
        return Optional.ofNullable(values.get(ItemTypeEx.of(stack)))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())))
                .orElse(null);
    }

    static BigInteger getValueRaw(ItemStackSnapshot stack) {
        return Optional.ofNullable(values.get(ItemTypeEx.of(stack)))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())))
                .orElse(null);
    }

    public static BigInteger getValueFor(Inventory inventory) {
        BigInteger value = BigInteger.ZERO;
        for (Inventory gfd : inventory.slots()) {
            Optional<BigInteger> stackValue = gfd.peek()
                    .flatMap(Calculator::getValueFor);
            if (stackValue.isPresent()) {
                value = value.add(stackValue.get());
            }
        }
        return value;
    }

    /** returns if a value was manually set */
    public static boolean containsValue(ItemTypeEx type) {
        return values.containsKey(type);
    }
    /** @return true if the price was calculated, false if it's a manually set / default value */
    public static boolean isCalculatedPrice(ItemTypeEx type) {
        return !fixedCost.contains(type);
    }

    /** @return the single biggest itemstack that can be created of the given type
     * with the specified amount of EMC */
    public static ItemStack getMaxStack(ItemTypeEx item, BigInteger available) {
        Optional<BigInteger> base = getValueFor(item);
        if (!base.isPresent() || base.get().compareTo(BigInteger.ZERO)<=0)
            return ItemStack.empty();
        BigInteger amount = available.divide(base.get()).min(BigInteger.valueOf(item.getType().getMaxStackQuantity()));
        return item.itemStack(amount.intValue());
    }

}
