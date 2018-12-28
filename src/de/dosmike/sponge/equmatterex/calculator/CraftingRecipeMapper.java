package de.dosmike.sponge.equmatterex.calculator;

import de.dosmike.sponge.equmatterex.EquivalentMatter;
import de.dosmike.sponge.equmatterex.ItemTypeEx;
import de.dosmike.sponge.equmatterex.inventoryWrappers.CraftingInventoryFactory;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class CraftingRecipeMapper implements ItemValueMapper<CraftingRecipe> {

    public CraftingRecipeMapper(Collection<CraftingRecipe> collection) {
        input = collection;
    }

    private Collection<CraftingRecipe> input;
    private Map<ItemTypeEx, BigInteger> compResult = new HashMap<>();

    @Override
    public Map<ItemTypeEx, BigInteger> call() {
        return compute(input);
    }

    @Override
    public Map<ItemTypeEx, BigInteger> compute(Collection<CraftingRecipe> recipes) {
            for (CraftingRecipe recipe : recipes) {
                ItemTypeEx result = ItemTypeEx.of(recipe.getExemplaryResult());
                Map<ItemTypeEx, BigInteger> recalc;
                if (recipe instanceof ShapedCraftingRecipe ||
                    recipe instanceof ShapelessCraftingRecipe) {
                    recalc = mutateRecipe(recipe);
                } else {
                    if (Calculator.verbosity>0) EquivalentMatter.l("Skipping %s for type %s", result.getId(), recipe.getClass().getSimpleName());
                    continue;
                }
                for (Map.Entry<ItemTypeEx, BigInteger> calculated : recalc.entrySet()) {
                    if (Calculator.isCalculatedPrice(calculated.getKey())) {
                        //lambda present if value in calculator and value less calculated: if a cheaper price is stored
                        //if not present a higher or no price is stored -> return the calculated one
                        if (!Optional.ofNullable(Calculator.getValueRaw(calculated.getKey())).filter(value->value.compareTo(calculated.getValue())<=0).isPresent()) {
                            compResult.put(calculated.getKey(), calculated.getValue());
                            if (Calculator.verbosity>0) EquivalentMatter.w("Updated %s to %s EMC", calculated.getKey().getId(), calculated.getValue().toString());
                        }
                    }
                }
            }
        return compResult;
    }

    private int[][] collectRecipeDimension(ShapedCraftingRecipe recipe) {
        int[] size = new int[recipe.getHeight()*recipe.getWidth()];
        int[] i = new int[recipe.getHeight()*recipe.getWidth()];
        Arrays.fill(i,0);
        for (int x = 0; x < recipe.getWidth(); x++)
            for (int y = 0; y < recipe.getHeight(); y++) {
                List<ItemTypeEx> inSlot = ingridientType(recipe.getIngredient(x,y));
                size[y*recipe.getWidth()+x] = inSlot.size();
            }
        return new int[][]{size, i};
    }
    private int[][] collectRecipeDimension(ShapelessCraftingRecipe recipe) {
        int[] size = new int[recipe.getIngredientPredicates().size()];
        int[] i = new int[size.length];
        Arrays.fill(i,0);
        for (int x = 0; x < size.length; x++) {
            List<ItemTypeEx> inSlot = ingridientType(recipe.getIngredientPredicates().get(x));
            size[x] = inSlot.size();
        }
        return new int[][]{size, i};
    }
    /** this function is not pure! */
    private void populateGrid(CraftingGridInventory inv, List<ItemTypeEx> in, ShapedCraftingRecipe recipe, int[] indices) {
        inv.clear();
        in.clear();
        for (int y = 0; y < recipe.getHeight(); y++)
            for (int x = 0; x < recipe.getWidth(); x++) {
                List<ItemTypeEx> inSlot = ingridientType(recipe.getIngredient(x,y));
                if (inSlot.size() > 0) {
                    ItemTypeEx current = inSlot.get(indices[y*recipe.getWidth()+x]);
                    inv.set(x, y, current.itemStack());
                    in.add(current);
                } else {
                    in.add(ItemTypeEx.of(ItemTypes.AIR));
                }
            }
    }
    /** this function is not pure! */
    private void populateGrid(CraftingGridInventory inv, List<ItemTypeEx> in, ShapelessCraftingRecipe recipe, int[] indices) {
        inv.clear();
        in.clear();
        for (int x = 0; x < indices.length; x++) {
            List<ItemTypeEx> inSlot = ingridientType(recipe.getIngredientPredicates().get(x));
            if (inSlot.size() > 0) {
                ItemTypeEx current = inSlot.get(indices[x]);
                inv.set(x % 3, x / 3, current.itemStack());
                in.add(current);
            } else {
                in.add(ItemTypeEx.of(ItemTypes.AIR));
            }
        }
    }
    private <T> void count(Map<T, Integer> map, T object) {
        int c = map.getOrDefault(object, 0);
        map.put(object, ++c);
    }
    private <T> int getCount(Map<T, Integer> map, T object) {
        return map.getOrDefault(object, 0);
    }
    private Map<ItemTypeEx, BigInteger> mutateRecipe(CraftingRecipe recipe) {
//        World world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        Map<ItemTypeEx, BigInteger> results = new HashMap<>();
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
        if (Calculator.verbosity>1) EquivalentMatter.l("Mutations for exemplary %s : %s", ItemTypeEx.of(recipe.getExemplaryResult()).getId(), Arrays.toString(size));
        for (; mutations<totalMutations; mutations++) {
            //setprobe
            List<ItemTypeEx> in = new LinkedList<>();
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

            if (Calculator.verbosity>2) EquivalentMatter.l("Checking %s:\n%s", shaped?"shaped":"shapeless", printRecipe(in, result, shaped?((ShapedCraftingRecipe)recipe).getWidth():0));
            //calculate cost
            BigInteger cost = BigInteger.ZERO;
            Map<ItemTypeEx, Integer> missingTypes = new HashMap<>();
            int safety=0;
            for (Inventory inv : probe.slots()) {
                if (++safety>9) throw new RuntimeException("More than 9 slots iterated!");
                if (!inv.peek().isPresent()||
                        inv.peek().get().getQuantity()<1||
                        inv.peek().get().getType().equals(ItemTypes.AIR))
                    continue;
                if (!Calculator.containsValue(ItemTypeEx.of(inv.peek().get()))) {
                    count(missingTypes, ItemTypeEx.of(inv.peek().get()));
                } else {
                    cost = cost.add(Calculator.getValueRaw(ItemTypeEx.of(inv.peek().get())));
                }
            }
            if (missingTypes.size()==0) {
                cost = cost.divide(BigInteger.valueOf(result.getQuantity()));
                if (Calculator.verbosity>1) EquivalentMatter.l("Estimated value for %s: %s", result.getTranslation().get(), cost.toString());
                if (!results.containsKey(ItemTypeEx.of(result)) || cost.compareTo(results.get(ItemTypeEx.of(result)))<0) {
                    results.put(ItemTypeEx.of(result), cost);
                } else {
                    if (Calculator.verbosity>1) EquivalentMatter.l("Can't determine value for %s", result.getTranslation().get());
                }
            } else if (missingTypes.size()==1 && Calculator.containsValue(ItemTypeEx.of(result))) {
                //if only one input type is missing and we know the result value (e.g. log -> planks)
                //we can reverse calculate the value for the left side
                BigInteger resultCost = Calculator.getValueRaw(result);
                ItemTypeEx missing = missingTypes.keySet().iterator().next();
                int inputDivider = missingTypes.get(missing);
                //subtract all other input costs from the result to get the value of the missing type input
                for (Inventory inv : probe.slots()) {
                    if (!inv.peek().isPresent()||
                            inv.peek().get().getQuantity()<1||
                            inv.peek().get().getType().equals(ItemTypes.AIR)||
                            ItemTypeEx.of(inv.peek().get()).equals(missing)) continue;
                    BigInteger slotValue = Calculator.getValueRaw(inv.peek().get());
                    if (slotValue.compareTo(resultCost)>0)
                        resultCost = BigInteger.ZERO;
                    else
                        resultCost = resultCost.subtract(slotValue);
                }
                resultCost = resultCost.divide(BigInteger.valueOf(inputDivider));
                if (resultCost.compareTo(BigInteger.ZERO)>0) {
                    if (Calculator.verbosity>1) EquivalentMatter.l("Reversed value of %s to %s", missing.getId(), resultCost.toString());
                    if (!results.containsKey(ItemTypeEx.of(result)) || cost.compareTo(results.get(ItemTypeEx.of(result)))<0)
                        results.put(missing, resultCost);
                } else {
                    if (Calculator.verbosity>1) EquivalentMatter.l("Failed to reverse value for %s (would be <1)", missing.getId());
                }
            }

            //mutate
            for (int n = size.length-1; n>=0;) {
                if (++i[n] >= size[n]) {
                    i[n] = 0;
                    n--;
                } else break;
            }
            if (System.currentTimeMillis() - t0 > Calculator.logStillWorkingNotificationDelay) {
                if (Calculator.verbosity>0) EquivalentMatter.w("Checking mutations for %s: %.2f%% (%d/%d) done...", result.getTranslation().get(), (double) mutations*100.0 / totalMutations, mutations, totalMutations);
                t0 = System.currentTimeMillis();
            }
        }
        return results;
    }

    private List<ItemTypeEx> ingridientType(Ingredient ingredient) {
        List<ItemTypeEx> types = new LinkedList<>();
        for (ItemStackSnapshot snap : ingredient.displayedItems()) {
            if (!types.contains(ItemTypeEx.of(snap)))
                types.add(ItemTypeEx.of(snap));
        }
        return types;
    }

    private String printRecipe(List<ItemTypeEx> input, ItemStackSnapshot output, int width) {
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
            Map<ItemTypeEx, Character> chars = new HashMap<>();
            for (ItemTypeEx i : input)
                if (i != null && !i.getType().equals(ItemTypes.AIR) && !chars.containsKey(i)) {
                    chars.put(i, pre.remove(0));
                }

            StringBuilder aile = new StringBuilder();
            for (int i = 0, x = 0; i < input.size(); i++, x++) {
                if (x >= width) {
                    aile.append('\n');
                    x = 0;
                }
                if (input.get(i) == null || input.get(i).getType().equals(ItemTypes.AIR)) {
                    aile.append(' ');
                } else {
                    aile.append(chars.get(input.get(i)));
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
