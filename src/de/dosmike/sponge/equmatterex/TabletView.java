package de.dosmike.sponge.equmatterex;

import de.dosmike.sponge.equmatterex.calculator.Calculator;
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
import org.spongepowered.api.text.format.TextStyles;

import java.math.BigInteger;
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
                if ((slot >= 21 && slot <= 23) || slot == 26) {
                    //clicked buttons
                    transaction.setValid(false);
                    event.setCancelled(true);
                    int action = 0;
                    if (slot == 21) action = -1; //left button
                    if (slot == 23) action = 1; //right button

                    if (action <0 && TabletView.this.page> 1) {
                        TabletView.this.page --;
                        requireUpdate.set(true);
                    } else if (action >0 &&
                            containing.query(SlotIndex.of(17)).totalItems()>0) {
                        //if there's still items in the last display slot we go to the next page
                        TabletView.this.page ++;
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

                            if (take) {
                                EMCAccount.withdraw(TabletView.this.player, value.get());
                                EquivalentMatter.l("%s take %d %s for %s", TabletView.this.player.getName(), deltaStack.getQuantity(), deltaStack.getTranslation().get(), value.get());
                            } else {
                                if (EMCAccount.learn(TabletView.this.player, ItemTypeEx.of(deltaStack))) {
                                    EquivalentMatter.l("%s check knowledge for %s: learned", TabletView.this.player.getName(), deltaStack.getTranslation().get());
                                    TabletView.this.player.playSound(SoundTypes.ENTITY_PLAYER_LEVELUP, SoundCategories.PLAYER, TabletView.this.player.getPosition(), 1.0, 1.0);
                                    TabletView.this.player.sendMessage(Text.of("You learned ", TextColors.AQUA, slotAfter.getType().getName(), TextColors.RESET));
                                } else {
                                    EquivalentMatter.l("%s check knowledge for %s: known", TabletView.this.player.getName(), deltaStack.getTranslation().get());
                                }
                                EMCAccount.deposit(TabletView.this.player, value.get());
                                EquivalentMatter.l("%s burn %d %s for %s", TabletView.this.player.getName(), deltaStack.getQuantity(), deltaStack.getTranslation().get(), value.get());
                            }
                        }
                    } else {
                        // "burned" was stack after, taken was stack before
                        Optional<BigInteger> burnValue = Calculator.getValueFor(slotAfter);
                        Optional<BigInteger> takeValue = Calculator.getValueFor(slotBefore);
                        if (!burnValue.isPresent() || burnValue.get().compareTo(BigInteger.ZERO)<=0) {
                            transaction.setValid(false);
                            event.setCancelled(true);
                        } else {
                            requireUpdate.set(true);
                            if (EMCAccount.learn(TabletView.this.player, ItemTypeEx.of(slotAfter))) {
                                EquivalentMatter.l("%s check knowledge for %s: learned", TabletView.this.player.getName(), slotAfter.getTranslation().get());
                                TabletView.this.player.playSound(SoundTypes.ENTITY_PLAYER_LEVELUP, SoundCategories.PLAYER, TabletView.this.player.getPosition(), 1.0, 1.0);
                                TabletView.this.player.sendMessage(Text.of("You learned ", TextColors.AQUA, slotAfter.getType().getName(), TextColors.RESET));
                            } else {
                                EquivalentMatter.l("%s check knowledge for %s: known", TabletView.this.player.getName(), slotAfter.getTranslation().get());
                            }
                            EquivalentMatter.l("%s burn %d %s for %s", TabletView.this.player.getName(), slotAfter.getQuantity(), slotAfter.getTranslation().get(), burnValue.get());
                            EquivalentMatter.l("%s take %d %s for %s", TabletView.this.player.getName(), slotBefore.getQuantity(), slotBefore.getTranslation().get(), takeValue.get());
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

    int page;
    Player player;

    void renderPage(Inventory inv) {
        EquivalentMatter.l("Refreshing page %d for %s", page, player.getName());
        BigInteger balance = EMCAccount.getBalance(player);

        Inventory slot;
        int i=0;
        for (ItemTypeEx item : EMCAccount.getKnowledgePage(player, page, 18, balance)) {
            ItemStack stack = Calculator.getMaxStack(item, balance);
            if (!stack.isEmpty()) {
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
                        .add(Keys.DISPLAY_NAME, Text.of(TextStyles.RESET, "<< Back"))
                .build()
        );
        slot = inv.query(SlotIndex.of(22));
        slot.clear();
        slot.offer(
                ItemStack.builder()
                        .itemType(ItemTypes.PAPER)
                        .quantity(page)
                        .add(Keys.DISPLAY_NAME, Text.of(TextStyles.RESET, "Page ", page))
                        .build()
        );
        slot = inv.query(SlotIndex.of(23));
        slot.clear();
        slot.offer(
                ItemStack.builder()
                        .itemType(ItemTypes.ARROW)
                        .add(Keys.DISPLAY_NAME, Text.of(TextStyles.RESET, "Next >>"))
                        .build()
        );
        slot = inv.query(SlotIndex.of(26));
        slot.clear();
        slot.offer (
                ItemStack.builder()
                        .itemType(ItemTypes.SIGN)
                        .add(Keys.DISPLAY_NAME, Text.of(balance.toString(), TextColors.YELLOW, " EMC"))
                        .build()
        );
    }



}
