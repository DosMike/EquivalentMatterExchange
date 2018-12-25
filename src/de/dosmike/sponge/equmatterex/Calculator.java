package de.dosmike.sponge.equmatterex;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.equmatterex.inventoryWrappers.CraftingInventoryFactory;
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
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.item.recipe.smelting.SmeltingRecipe;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Calculator {

    private static Map<ItemType, BigInteger> values = new HashMap<>();
    private static Set<ItemType> fixedCost = new HashSet<>();
    private static int verbosity = 3;
    public static void setVerbosity(int level) {
        if (level < 0 || level > 3)
            throw new IllegalArgumentException("Illegal verbosity level");
        verbosity = level;
    }

    /** calling this requires recalculation */
    public static void resetAndDefaults() {
        values.clear();
        fixedCost.clear();

        setFixCost(ItemTypes.COBBLESTONE, 1);
        setFixCost(ItemTypes.STONE, 1);
        setFixCost(ItemTypes.STONEBRICK, 1);
        setFixCost(ItemTypes.END_STONE, 1);
        setFixCost(ItemTypes.NETHERRACK, 1);
        setFixCost(ItemTypes.DIRT, 1);
        setFixCost(ItemTypes.SAND, 1);
        setFixCost(ItemTypes.SNOW, 1);
        setFixCost(ItemTypes.ICE, 1);
        setFixCost(ItemTypes.DEADBUSH, 1);
        setFixCost(ItemTypes.GRAVEL, 4);
        setFixCost(ItemTypes.CACTUS, 8);
        setFixCost(ItemTypes.VINE, 8);
        setFixCost(ItemTypes.WEB, 12);
        setFixCost(ItemTypes.WATERLILY, 16);
        setFixCost(ItemTypes.RED_FLOWER, 16);
        setFixCost(ItemTypes.DOUBLE_PLANT, 32);
        setFixCost(ItemTypes.YELLOW_FLOWER, 16);
        setFixCost(ItemTypes.RED_MUSHROOM, 32);
        setFixCost(ItemTypes.BROWN_MUSHROOM, 32);
        setFixCost(ItemTypes.REEDS, 32);
        setFixCost(ItemTypes.SOUL_SAND, 49);
        setFixCost(ItemTypes.OBSIDIAN, 64);
        setFixCost(ItemTypes.STAINED_HARDENED_CLAY, 64);
        setFixCost(ItemTypes.SPONGE, 128);
        setFixCost(ItemTypes.TALLGRASS, 1);
        setFixCost(ItemTypes.PACKED_ICE, 4);
        setFixCost(ItemTypes.MAGMA, 128);

        setFixCost(ItemTypes.CHORUS_PLANT, 64);
        setFixCost(ItemTypes.CHORUS_FLOWER, 96);
        setFixCost(ItemTypes.WHEAT_SEEDS, 16);
        setFixCost(ItemTypes.BEETROOT_SEEDS, 16);
        setFixCost(ItemTypes.MELON, 16);
        setFixCost(ItemTypes.WHEAT, 24);
        setFixCost(ItemTypes.NETHER_WART, 24);
        setFixCost(ItemTypes.APPLE, 128);
        setFixCost(ItemTypes.PUMPKIN, 144);
        setFixCost(ItemTypes.PORKCHOP, 64);
        setFixCost(ItemTypes.BEEF, 64);
        setFixCost(ItemTypes.CHICKEN, 64);
        setFixCost(ItemTypes.RABBIT, 64);
        setFixCost(ItemTypes.MUTTON, 64);
        setFixCost(ItemTypes.FISH, 64);
        setFixCost(ItemTypes.CARROT, 64);
        setFixCost(ItemTypes.BEETROOT, 64);
        setFixCost(ItemTypes.POTATO, 64);
        setFixCost(ItemTypes.POISONOUS_POTATO, 64);
        setFixCost(ItemTypes.CHORUS_FRUIT, 192);

        setFixCost(ItemTypes.STRING, 12);
        setFixCost(ItemTypes.ROTTEN_FLESH, 32);
        setFixCost(ItemTypes.SLIME_BALL, 32);
        setFixCost(ItemTypes.EGG, 32);
        setFixCost(ItemTypes.FEATHER, 48);
        setFixCost(ItemTypes.RABBIT_HIDE, 16);
        setFixCost(ItemTypes.RABBIT_FOOT, 128);
        setFixCost(ItemTypes.SPIDER_EYE, 128);
        setFixCost(ItemTypes.GUNPOWDER, 192);
        setFixCost(ItemTypes.ENDER_PEARL, 1024);
        setFixCost(ItemTypes.BLAZE_ROD, 1536);
        setFixCost(ItemTypes.GHAST_TEAR, 4096);
        setFixCost(ItemTypes.DRAGON_EGG, 262144);
        setFixCost(ItemTypes.POTION, 0);

        setFixCost(ItemTypes.STICK, 4);
        setFixCost(ItemTypes.SADDLE, 192);
        setFixCost(ItemTypes.NAME_TAG, 192);
        setFixCost(ItemTypes.RECORD_11, 2048);
        setFixCost(ItemTypes.RECORD_13, 2048);
        setFixCost(ItemTypes.RECORD_BLOCKS, 2048);
        setFixCost(ItemTypes.RECORD_CAT, 2048);
        setFixCost(ItemTypes.RECORD_CHIRP, 2048);
        setFixCost(ItemTypes.RECORD_FAR, 2048);
        setFixCost(ItemTypes.RECORD_MALL, 2048);
        setFixCost(ItemTypes.RECORD_MELLOHI, 2048);
        setFixCost(ItemTypes.RECORD_STAL, 2048);
        setFixCost(ItemTypes.RECORD_STRAD, 2048);
        setFixCost(ItemTypes.RECORD_WAIT, 2048);
        setFixCost(ItemTypes.RECORD_WARD, 2048);

        setFixCost(ItemTypes.IRON_INGOT, 256);
        setFixCost(ItemTypes.GOLD_INGOT, 2048);
        setFixCost(ItemTypes.DIAMOND, 8192);
        setFixCost(ItemTypes.FLINT, 4);
        setFixCost(ItemTypes.COAL, 128);
        setFixCost(ItemTypes.REDSTONE, 64);
        setFixCost(ItemTypes.GLOWSTONE_DUST, 384);
        setFixCost(ItemTypes.QUARTZ, 256);
        setFixCost(ItemTypes.PRISMARINE_SHARD, 256);
        setFixCost(ItemTypes.PRISMARINE_CRYSTALS, 512);
        setFixCost(ItemTypes.DYE, 16);
        setFixCost(ItemTypes.ENCHANTED_BOOK, 2048);
        setFixCost(ItemTypes.EMERALD, 16384);
        setFixCost(ItemTypes.NETHER_STAR, 139264);
        setFixCost(ItemTypes.CLAY_BALL, 16);
        setFixCost(ItemTypes.BONE, 144);
        setFixCost(ItemTypes.SNOWBALL, 1);
        setFixCost(ItemTypes.FILLED_MAP, 1472);

        try {
            ConfigurationLoader<CommentedConfigurationNode> defaultCfg = HoconConfigurationLoader.builder()
                    .setPath(EquivalentMatter.getInstance().getConfigDir().resolve("defaultValues.conf"))
                    .build();

            CommentedConfigurationNode root = defaultCfg.createEmptyNode();
            root.setComment("This configuration lists the internal default values and is meant as reference!\nChanging this configuration has no effect, as it will be rewritten every time the values regenerate");
            Map<String, String> values = new HashMap<>();
            Calculator.getAllValues().forEach((key, value) -> values.put(key.getType().getId(), value.toString()));
            root.getNode("presets").setValue(new TypeToken<Map<String, String>>(){}, values);
            defaultCfg.save(root);
        } catch (ObjectMappingException | IOException e) {
            EquivalentMatter.w("Failed to write defaults");
            e.printStackTrace();
        }
    }
    static Map<ItemType, BigInteger> getAllValues() {
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
            Calculator.getAllValues().forEach((key, value) -> (fixedCost.contains(key.getType())?presets:calculated).put(key.getType().getId(), value.toString()));
            root.getNode("presets").setValue(new TypeToken<Map<String, String>>(){}, presets);
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
    public static void setFixCost(ItemType item, BigInteger cost) {
        fixedCost.add(item);
        values.put(item, cost);
    }

    /** calling this requires recalculation */
    public static void setFixCost(ItemType item, int cost) {
        setFixCost(item, BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setFixCost(ItemType item, long cost) {
        setFixCost(item, BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemType item, BigInteger cost) {
        values.put(item, cost);
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemType item, int cost) {
        setTemporaryCost(item, BigInteger.valueOf(cost));
    }

    /** calling this requires recalculation */
    public static void setTemporaryCost(ItemType item, long cost) {
        setTemporaryCost(item, BigInteger.valueOf(cost));
    }

    /** removes fixed status and allows the value to be calculated again */
    public static boolean resetCost(ItemType item) {
        return fixedCost.remove(item);
    }

    private static AtomicBoolean calculating = new AtomicBoolean(false);
    public static boolean isCalculating() {
        return calculating.get();
    }
    /** break glass in case of exception */
    static void resetState() {
        calculating.set(false);
    }
    public static void calculate() {
        if (calculating.getAndSet(true))
            throw new IllegalStateException("EMC values are already rebuilding");

        int changes = 1, maxIterations = 10;

        for (int iteration = 0; iteration < maxIterations && changes > 0; iteration++) {
            if (verbosity>0) EquivalentMatter.w("Iteration %d/%d:", iteration+1, maxIterations);
            changes = 0;
            int scanned=0;
            long t0 = System.currentTimeMillis();
            {
                Collection<CraftingRecipe> craftingRecipes = Sponge.getRegistry().getCraftingRecipeRegistry().getRecipes();
                for (CraftingRecipe recipe : craftingRecipes) {
                    ItemType result = recipe.getExemplaryResult().getType();
                    Map<ItemType, BigInteger> recalc;
                    if (recipe instanceof ShapedCraftingRecipe ||
                        recipe instanceof ShapelessCraftingRecipe) {
                        recalc = mutateRecipe(recipe);
                    } else {
                        if (verbosity>0) EquivalentMatter.l("Skipping %s for type %s", result.getType().getId(), recipe.getClass().getSimpleName());
                        continue;
                    }
                    for (Map.Entry<ItemType, BigInteger> calculated : recalc.entrySet()) {
                        if (!fixedCost.contains(calculated.getKey())) {
                            if (!values.containsKey(calculated.getKey()) || values.get(calculated.getKey()).compareTo(calculated.getValue()) > 0) {
                                values.put(calculated.getKey(), calculated.getValue());
                                if (verbosity>0) EquivalentMatter.w("Updated %s to %s EMC", calculated.getKey().getId(), calculated.getValue().toString());
                                changes++;
                            }
                        }
                    }

                    scanned++;
                    if (System.currentTimeMillis() - t0 > 5000) {
                        if (verbosity>0) EquivalentMatter.w("Crafting scan %.2f%% (%d/%d) done...", (double) scanned / craftingRecipes.size(), scanned, craftingRecipes.size());
                        t0 = System.currentTimeMillis();
                    }
                }
            }
            scanned = 0;
            {
                Collection<CraftingRecipe> craftingRecipes = Sponge.getRegistry().getCraftingRecipeRegistry().getRecipes();
                if (values.containsKey(ItemTypes.COAL)) {//min required to calculate
                    //one coal block can smelt 8 items
                    BigInteger costPerSmelt = values.get(ItemTypes.COAL).divide(BigInteger.valueOf(8));
                    for (SmeltingRecipe recipe : Sponge.getGame().getRegistry().getSmeltingRecipeRegistry().getRecipes()) {
                        if (verbosity>2) EquivalentMatter.l("Checking Smelting:\n%s\n => %s x%d", recipe.getExemplaryIngredient().getType().getId(), recipe.getExemplaryResult().getType().getId(), recipe.getExemplaryResult().getQuantity());
                        BigInteger cost = values.get(recipe.getExemplaryIngredient().getType());
                        if (cost == null) continue;
                        cost = cost.add(costPerSmelt);
                        ItemType output = recipe.getExemplaryResult().getType();
                        if (fixedCost.contains(output)) continue;

                        BigInteger minimalValue = values.get(output.getType());
                        if (minimalValue == null || minimalValue.compareTo(cost) > 0) {
                            values.put(output.getType(), cost);
                            if (verbosity>0) EquivalentMatter.w("Updated %s to %s EMC", output.getType().getId(), cost.toString());
                            changes++;
                        }

                        scanned++;
                        if (System.currentTimeMillis() - t0 > 5000) {
                            if (verbosity>0) EquivalentMatter.w("Smelting scan %.2f%% (%d/%d) done...", (double) scanned*100.0 / craftingRecipes.size(), scanned, craftingRecipes.size());
                            t0 = System.currentTimeMillis();
                        }
                    }
                }
            }
            EquivalentMatter.l("[Calculator] Updated %d EMC values", changes);
        }
        if (changes > 0) {
            EquivalentMatter.w("[Calculator] Could not resolve all costs after %d iterations!", maxIterations);
        }
        Collection<String> missing = Sponge.getRegistry()
                .getAllOf(ItemType.class)
                .stream()
                .filter(it->!values.containsKey(it))
                .map(ItemType::getName)
                .collect(Collectors.toList());
        if (missing.size()>0) {
            EquivalentMatter.w("[Calculator] The following items are still missing: %s ", String.join(", ", missing));
        }

        EquivalentMatter.l("Saving calculated values...");
        safeValues();

        calculating.set(false);
    }

    private static int[][] collectRecipeDimension(ShapedCraftingRecipe recipe) {
        int[] size = new int[recipe.getHeight()*recipe.getWidth()];
        int[] i = new int[recipe.getHeight()*recipe.getWidth()];
        Arrays.fill(i,0);
        for (int x = 0; x < recipe.getWidth(); x++)
            for (int y = 0; y < recipe.getHeight(); y++) {
                List<ItemType> inSlot = ingridientType(recipe.getIngredient(x,y));
                size[y*recipe.getWidth()+x] = inSlot.size();
            }
        return new int[][]{size, i};
    }
    private static int[][] collectRecipeDimension(ShapelessCraftingRecipe recipe) {
        int[] size = new int[recipe.getIngredientPredicates().size()];
        int[] i = new int[size.length];
        Arrays.fill(i,0);
        for (int x = 0; x < size.length; x++) {
            List<ItemType> inSlot = ingridientType(recipe.getIngredientPredicates().get(x));
            size[x] = inSlot.size();
        }
        return new int[][]{size, i};
    }
    /** this function is not pure! */
    private static void populateGrid(CraftingGridInventory inv, List<ItemType> in, ShapedCraftingRecipe recipe, int[] indices) {
        inv.clear();
        in.clear();
        for (int y = 0; y < recipe.getHeight(); y++)
            for (int x = 0; x < recipe.getWidth(); x++) {
                List<ItemType> inSlot = ingridientType(recipe.getIngredient(x,y));
                if (inSlot.size() > 0) {
                    ItemType current = inSlot.get(indices[y*recipe.getWidth()+x]);
                    inv.set(x, y, ItemStack.of(current, 1));
                    in.add(current);
                } else {
                    in.add(ItemTypes.AIR);
                }
            }
    }
    /** this function is not pure! */
    private static void populateGrid(CraftingGridInventory inv, List<ItemType> in, ShapelessCraftingRecipe recipe, int[] indices) {
        inv.clear();
        in.clear();
        for (int x = 0; x < indices.length; x++) {
            List<ItemType> inSlot = ingridientType(recipe.getIngredientPredicates().get(x));
            if (inSlot.size() > 0) {
                ItemType current = inSlot.get(indices[x]);
                inv.set(x % 3, x / 3, ItemStack.of(current, 1));
                in.add(current);
            } else {
                in.add(ItemTypes.AIR);
            }
        }
    }
    private static <T> void count(Map<T, Integer> map, T object) {
        int c = map.getOrDefault(object, 0);
        map.put(object, ++c);
    }
    private static <T> int getCount(Map<T, Integer> map, T object) {
        return map.getOrDefault(object, 0);
    }
    private static Map<ItemType, BigInteger> mutateRecipe(CraftingRecipe recipe) {
//        World world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        Map<ItemType, BigInteger> results = new HashMap<>();
        boolean shaped = recipe instanceof ShapedCraftingRecipe;

        int[] size, i;
        {
            int[][] r = shaped
                    ? collectRecipeDimension((ShapedCraftingRecipe)recipe)
                    : collectRecipeDimension((ShapelessCraftingRecipe)recipe);
            size=r[0];
            i=r[1];
        }

        CraftingGridInventory probe = CraftingInventoryFactory.buildGrid();

        long t0 = System.currentTimeMillis();
        long totalMutations = 1, mutations = 0;
        for (int c : size) {
            if (c>0) //air slots have 0 mutations at this point
                totalMutations *= c;
        }
        if (verbosity>1) EquivalentMatter.l("Mutations for exemplary %s : %s", recipe.getExemplaryResult().getType().getId(), Arrays.toString(size));
        for (; mutations<totalMutations; mutations++) {
            //setprobe
            List<ItemType> in = new LinkedList<>();
            if (shaped)
                populateGrid(probe, in, (ShapedCraftingRecipe)recipe, i);
            else
                populateGrid(probe, in, (ShapelessCraftingRecipe)recipe, i);
            //probe recipe
            ItemStackSnapshot result;
//            if (recipe.isValid(probe, world))
//                result = recipe.getResult(probe);
//            else
                result = recipe.getExemplaryResult();

            if (verbosity>2) EquivalentMatter.l("Checking %s:\n%s", shaped?"shaped":"shapeless", printRecipe(in, result, shaped?((ShapedCraftingRecipe)recipe).getWidth():0));
            //calculate cost
            BigInteger cost = BigInteger.ZERO;
            Map<ItemType, Integer> missingTypes = new HashMap<>();
            int safety=0;
            for (Inventory inv : probe.slots()) {
                if (++safety>9) throw new RuntimeException("More than 9 slots iterated!");
                if (!inv.peek().isPresent()||
                    inv.peek().get().getQuantity()<1||
                    inv.peek().get().getType().equals(ItemTypes.AIR))
                    continue;
                if (!values.containsKey(inv.peek().get().getType())) {
                    count(missingTypes, inv.peek().get().getType());
                } else {
                    cost = cost.add(values.get(inv.peek().get().getType()));
                }
            }
            if (missingTypes.size()==0) {
                cost = cost.divide(BigInteger.valueOf(result.getQuantity()));
                if (verbosity>1) EquivalentMatter.l("Estimated value: %s", cost.toString());
                if (!results.containsKey(result.getType()) || cost.compareTo(results.get(result.getType()))<0) {
                    results.put(result.getType(), cost);
                } else {
                    if (verbosity>1) EquivalentMatter.l("Can't determine value");
                }
            } else if (missingTypes.size()==1 && values.containsKey(result.getType())) {
                //if only one input type is missing and we know the result value (e.g. log -> planks)
                //we can reverse calculate the value for the left side
                BigInteger resultCost = getValueFor(result).get();
                ItemType missing = missingTypes.keySet().iterator().next();
                int inputDivider = missingTypes.get(missing);
                //subtract all other input costs from the result to get the value of the missing type input
                for (Inventory inv : probe.slots()) {
                    if (!inv.peek().isPresent()||
                        inv.peek().get().getQuantity()<1||
                        inv.peek().get().getType().equals(ItemTypes.AIR)||
                        inv.peek().get().getType().equals(missing)) continue;
                    BigInteger slotValue = getValueFor(inv.peek().get()).get();
                    if (slotValue.compareTo(resultCost)>0)
                        resultCost = BigInteger.ZERO;
                    else
                        resultCost = resultCost.subtract(slotValue);
                }
                resultCost = resultCost.divide(BigInteger.valueOf(inputDivider));
                if (resultCost.compareTo(BigInteger.ZERO)>0) {
                    if (verbosity>1) EquivalentMatter.l("Reversed value of %s to %s", missing.getId(), resultCost.toString());
                    if (!results.containsKey(result.getType()) || cost.compareTo(results.get(result.getType()))<0)
                        results.put(missing, resultCost);
                } else {
                    if (verbosity>1) EquivalentMatter.l("Failed to reverse value for %s (would be <1)", missing.getId());
                }
            }

            //mutate
            for (int n = size.length-1; n>=0;) {
                if (++i[n] >= size[n]) {
                    i[n] = 0;
                    n--;
                } else break;
            }
            if (System.currentTimeMillis() - t0 > 5000) {
                if (verbosity>0) EquivalentMatter.w("Checking mutations %.2f%% (%d/%d) done...", (double) mutations*100.0 / totalMutations, mutations, totalMutations);
                t0 = System.currentTimeMillis();
            }
        }
        return results;
    }

    public static Optional<BigInteger> getValueFor(ItemType type) {
        return Optional.ofNullable(values.get(type));
    }

    public static Optional<BigInteger> getValueFor(ItemStack stack) {
        return Optional.ofNullable(values.get(stack.getType()))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())));
    }

    public static Optional<BigInteger> getValueFor(ItemStackSnapshot stack) {
        return Optional.ofNullable(values.get(stack.getType()))
                .map(v->v.multiply(BigInteger.valueOf(stack.getQuantity())));
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

    private static List<ItemType> ingridientType(Ingredient ingredient) {
        List<ItemType> types = new LinkedList<>();
        for (ItemStackSnapshot snap : ingredient.displayedItems()) {
            if (!types.contains(snap.getType()))
                types.add(snap.getType());
        }
        return types;
    }

    private static String printRecipe(List<ItemType> input, ItemStackSnapshot output, int width) {
        if (width > 0) {
            //generate aile
            List<Character> pre = new LinkedList<>();
            pre.add('a');
            pre.add('x');
            pre.add('i');
            pre.add('o');
            pre.add('n');
            pre.add('v');
            pre.add('t');
            pre.add('b');
            pre.add('z');
            Map<ItemType, Character> chars = new HashMap<>();
            for (ItemType i : input)
                if (i != null && !i.equals(ItemTypes.AIR) && !chars.containsKey(i.getType())) {
                    chars.put(i.getType(), pre.remove(0));
                }

            StringBuilder aile = new StringBuilder();
            for (int i = 0, x = 0; i < input.size(); i++, x++) {
                if (x >= width) {
                    aile.append('\n');
                    x = 0;
                }
                if (input.get(i) == null || input.get(i).equals(ItemTypes.AIR)) {
                    aile.append(' ');
                } else {
                    aile.append(chars.get(input.get(i).getType()));
                }
            }

            aile.append('\n');
            chars.forEach((k,v)->aile.append(String.format("%c: %s\n", v, k.getId())));
            aile.append(" => ");
            aile.append(output.getType().getName());
            aile.append(" x");
            aile.append(String.valueOf(output.getQuantity()));
            return aile.toString();
        } else {
            StringBuilder recipe = new StringBuilder();
            List<String> types = input.stream().map(s->s.getType().getId()).collect(Collectors.toList());
            recipe.append(String.join(", ", types));
            recipe.append("\n => ");
            recipe.append(output.getType().getName());
            recipe.append(" x");
            recipe.append(String.valueOf(output.getQuantity()));
            return recipe.toString();
        }
    }

}
