package de.dosmike.sponge.equmatterex;

import de.dosmike.sponge.equmatterex.calculator.Calculator;
import de.dosmike.sponge.equmatterex.emcDevices.TransmutationTable;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundCategories;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TabletView {

    private Consumer<ClickInventoryEvent> INVENTORY_CLICK = (event) -> {
        AtomicBoolean requireUpdate = new AtomicBoolean(false);
        Inventory containing = event.getTargetInventory().first();
        event.getTransactions().forEach(transaction->{
            //find index again
            Integer slot = transaction.getSlot().getInventoryProperty(SlotIndex.class).orElse(new SlotIndex(99)).getValue();
            if (slot!=null && slot < 27) {
                if (slot == 18 || (slot >= 21 && slot <= 23) || slot == 26) {
                    //clicked buttons
                    transaction.setValid(false);
                    event.setCancelled(true);

                    if (slot == 21 && TabletView.this.page> 1) { //left button
                        TabletView.this.page --;
                        requireUpdate.set(true);
                    } else if (slot == 23 && //right button
                            containing.query(SlotIndex.of(17)).totalItems()>0) {
                        //if there's still items in the last display slot we go to the next page
                        TabletView.this.page ++;
                        requireUpdate.set(true);
                    } else if (slot == 18) {//stack button
                        TabletView.this.maxStackDivider++;
                        if (TabletView.this.maxStackDivider>2)
                            TabletView.this.maxStackDivider=0;
                        //update slot in minimal future as the influenced slot can't be updated by renderPage()
                        // as the client is still busy cancelling the transaction
                        EquivalentMatter.getSyncExecutor().submit(()->{
                           Inventory islot = containing.query(SlotIndex.of(slot));
                           islot.clear();
                           islot.set(buildStackSizeDisplayItem());
                        });
                        requireUpdate.set(true);
                    }
                } else {
                    ItemStackSnapshot slotAfter = transaction.getFinal();
                    ItemStackSnapshot slotBefore = transaction.getOriginal();
                    if (slotBefore.getType().equals(slotAfter.getType()) ||
                            slotBefore.isEmpty() || slotAfter.isEmpty()) {
                        int amount = slotAfter.getQuantity()-slotBefore.getQuantity();
                        boolean take = amount < 0;
                        ItemStack deltaStack = ItemStack.builder().fromSnapshot(
                                take?slotBefore:slotAfter
                        ).quantity(Math.abs(amount)).build();
                        Optional<BigInteger> value = Calculator.getValueFor(deltaStack);
                        if (!value.isPresent() || value.get().compareTo(BigInteger.ZERO)<=0) {
                            transaction.setValid(false);
                            event.setCancelled(true);
                        } else {
                            requireUpdate.set(true);

                            ItemTypeEx type = ItemTypeEx.of(deltaStack);
                            if (take) {
                                EMCAccount.withdraw(TabletView.this.player, value.get());
                            } else if (!TransmutationTable.canLearn(type)) {
                                transaction.setValid(false);
                                event.setCancelled(true);
                                TabletView.this.player.sendMessage(Text.of("The mighty gods decided that this item shall not be learned"));
                            } else {
                                if (EMCAccount.learn(TabletView.this.player, type)) {
                                    TabletView.this.player.playSound(SoundTypes.ENTITY_PLAYER_LEVELUP, SoundCategories.PLAYER, TabletView.this.player.getPosition(), 1.0, 1.0);
                                    TabletView.this.player.sendMessage(Text.of("You learned ", TextColors.AQUA, slotAfter.getType().getName(), TextColors.RESET));
                                }
                                BigInteger burnValue = BigDecimal.valueOf(TransmutationTable.getEfficiency()).multiply(
                                        new BigDecimal(value.get())
                                ).toBigInteger();
                                EMCAccount.deposit(TabletView.this.player, burnValue);
                            }
                        }
                    } else {
                        // "burned" was stack after, taken was stack before
                        Optional<BigInteger> burnValue = Calculator.getValueFor(slotAfter).map(v->
                                BigDecimal.valueOf(TransmutationTable.getEfficiency()).multiply(
                                        new BigDecimal(v)
                                ).toBigInteger()
                        );
                        Optional<BigInteger> takeValue = Calculator.getValueFor(slotBefore);
                        if (!takeValue.isPresent()) {
                            transaction.setValid(false);
                            event.setCancelled(true);
                            EMCAccount.unlearn(TabletView.this.player, ItemTypeEx.of(slotBefore));
                        } else if (!burnValue.isPresent() || burnValue.get().compareTo(BigInteger.ZERO)<=0) {
                            transaction.setValid(false);
                            event.setCancelled(true);
                        } else {
                            requireUpdate.set(true);
                            if (EMCAccount.learn(TabletView.this.player, ItemTypeEx.of(slotAfter))) {
                                TabletView.this.player.playSound(SoundTypes.ENTITY_PLAYER_LEVELUP, SoundCategories.PLAYER, TabletView.this.player.getPosition(), 1.0, 1.0);
                                TabletView.this.player.sendMessage(Text.of("You learned ", TextColors.AQUA, slotAfter.getType().getName(), TextColors.RESET));
                            } else {
//                                EquivalentMatter.l("%s check knowledge for %s: known", TabletView.this.player.getName(), slotAfter.getTranslation().get());
                            }
                            BigInteger gain = burnValue.get().subtract(takeValue.get());
                            if (gain.compareTo(BigInteger.ZERO) > 0) {
                                EMCAccount.deposit(TabletView.this.player, gain);
                            } else if (gain.compareTo(BigInteger.ZERO)<0) {
                                EMCAccount.withdraw(TabletView.this.player, gain.negate());
                            } else {
                                //equal value
                            }
                        }
                    }
                }
            }
        });
        if (requireUpdate.get())
            renderPage(containing);
    };

    public TabletView(Player player) {
        this(player, 1);
    }

    public TabletView(Player player, int page) {
        Inventory display = Inventory.builder()
                .of(InventoryArchetypes.MENU_GRID)
                .property(InventoryTitle.of(Text.of("Transmutation Table")))
                .property(InventoryDimension.of(9,3))
                .listener(ClickInventoryEvent.class, INVENTORY_CLICK)
                .build(EquivalentMatter.getInstance());
        this.page = page;
        this.player = player;
        renderPage(display);
        player.openInventory(display);
    }

    private int page;
    private Player player;
    private int maxStackDivider = 1; //1 = full stacks, 2 = half stacks, 0 = 1 max

    ItemStack buildStackSizeDisplayItem() {
        return ItemStack.builder()
                .itemType(maxStackDivider==1
                        ?ItemTypes.IRON_BLOCK
                        :(maxStackDivider==2
                        ?ItemTypes.IRON_INGOT
                        :ItemTypes.IRON_NUGGET))
                .add(Keys.DISPLAY_NAME, Text.of("Extract Amount"))
                .add(Keys.ITEM_LORE, Arrays.asList(
                        Text.of(maxStackDivider==1?TextColors.YELLOW:TextColors.DARK_GRAY, "Full Stacks"),
                        Text.of(maxStackDivider==2?TextColors.YELLOW:TextColors.DARK_GRAY, "Half Stacks"),
                        Text.of(maxStackDivider==0?TextColors.YELLOW:TextColors.DARK_GRAY, "Single Items")
                ))
                .build();
    }

    void renderPage(Inventory inv) {
        BigInteger balance = EMCAccount.getBalance(player);

        Inventory slot;
        int i=0;
        for (ItemTypeEx item : EMCAccount.getKnowledgePage(player, page, 18, balance)) {
            ItemStack stack = Calculator.getMaxStack(item, balance);
            if (!stack.isEmpty()) {
                if (maxStackDivider!=1) stack.setQuantity(Math.max(1,Math.min(
                        stack.getQuantity(),
                        maxStackDivider<1?1:item.getType().getMaxStackQuantity()/maxStackDivider
                )));

                slot = inv.query(SlotIndex.of(i++));//don't touch, QueryOperations don't seem to work
                slot.clear();
                slot.offer(stack);
            }
        }
        //clear all other slots
        for (;i<26;i++) {
            inv.query(SlotIndex.of(i)).clear();
        }
        //add buttons
        slot = inv.query(SlotIndex.of(21));
        slot.clear();
        slot.offer(
                ItemStack.builder()
                        .itemType(ItemTypes.ARROW)
                        .add(Keys.DISPLAY_NAME, Text.of("<< Back"))
                .build()
        );
        slot = inv.query(SlotIndex.of(22));
        slot.clear();
        slot.offer(
                ItemStack.builder()
                        .itemType(ItemTypes.PAPER)
                        .quantity(page)
                        .add(Keys.DISPLAY_NAME, Text.of("Page ", page))
                        .build()
        );
        slot = inv.query(SlotIndex.of(23));
        slot.clear();
        slot.offer(
                ItemStack.builder()
                        .itemType(ItemTypes.ARROW)
                        .add(Keys.DISPLAY_NAME, Text.of("Next >>"))
                        .build()
        );
        slot = inv.query(SlotIndex.of(18));
        slot.clear();
        slot.offer(
                buildStackSizeDisplayItem()
        );
        slot = inv.query(SlotIndex.of(26));
        slot.clear();
        slot.offer (
                ItemStack.builder()
                        .itemType(ItemTypes.SIGN)
                        .add(Keys.DISPLAY_NAME, Text.of(TextColors.RESET, balance.toString(), TextColors.YELLOW, " EMC"))
                        .build()
        );

    }



}
