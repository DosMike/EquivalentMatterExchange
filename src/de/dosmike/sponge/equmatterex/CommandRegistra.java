package de.dosmike.sponge.equmatterex;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;

import java.math.BigInteger;
import java.util.Optional;

public class CommandRegistra {
    public static void registerCommands() {
        //region /emc
        Sponge.getCommandManager().register(EquivalentMatter.getInstance(), CommandSpec.builder()
                .permission("equmatex.command.emc")
                .description(Text.of("Check the EMC value of an item-type"))
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.catalogedElement(Text.of("ItemType"), ItemType.class)
                        )
                ).executor((src,args)->{
                    if (Calculator.isCalculating())
                        throw new CommandException(Text.of("Calculator is currently refreshing values"));
                    if (args.hasAny("ItemType")) {
                        ItemType type = args.<ItemType>getOne("ItemType").get();
                        Optional<BigInteger> value = Calculator.getValueFor(type);

                        if (value.isPresent()) {
                            src.sendMessage(Text.of(type.getName(), Text.NEW_LINE,
                                    TextColors.YELLOW, "EMC: ", TextColors.WHITE, value.get().toString()));
                        } else {
                            src.sendMessage(Text.of(type.getName(), Text.NEW_LINE,
                                    TextColors.YELLOW, "EMC: ", TextColors.WHITE, "Unknown"));
                        }
                    } else if (src instanceof Player) {
                        Player player = (Player)src;
                        Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
                        if (!item.isPresent())
                            throw new CommandException(Text.of("Please hold an item or pass a argument"));
                        ItemType type = item.get().getType();
                        Optional<BigInteger> value = Calculator.getValueFor(type);

                        if (value.isPresent()) {
                            if (item.get().getQuantity()>1)
                                src.sendMessage(Text.of(type.getName(), Text.NEW_LINE,
                                        TextColors.YELLOW, "Stack EMC: ", TextColors.WHITE, (value.get().multiply(BigInteger.valueOf(item.get().getQuantity()))).toString(), Text.NEW_LINE,
                                        TextColors.YELLOW, "EMC: ", TextColors.WHITE, value.get().toString()));
                            else
                                src.sendMessage(Text.of(type.getName(), Text.NEW_LINE,
                                        TextColors.YELLOW, "EMC: ", TextColors.WHITE, value.get().toString()));
                        } else {
                            src.sendMessage(Text.of(type.getName(), Text.NEW_LINE,
                                    TextColors.YELLOW, "EMC: ", TextColors.WHITE, "Unknown"));
                        }
                    } else {
                        throw new CommandException(Text.of("Terminal has to pass argument"));
                    }

                    return CommandResult.success();
                }).build(), "emc");
        //endregion
        //region /setEMC
        Sponge.getCommandManager().register(EquivalentMatter.getInstance(), CommandSpec.builder()
                .permission("equmatex.command.setemc")
                .description(Text.of("Fix the EMC value for some item, this may cause inconsistencies in recipe calculations!"))
                .arguments(
                        GenericArguments.catalogedElement(Text.of("ItemType"), ItemType.class),
                        GenericArguments.bigInteger(Text.of("NewValue"))
                ).executor((src,args)->{
                    if (Calculator.isCalculating())
                        throw new CommandException(Text.of("Calculator is currently refreshing values"));
                    ItemType type = args.<ItemType>getOne("ItemType").get();
                    BigInteger newVal = args.<BigInteger>getOne("NewValue").get();

                    Calculator.setFixCost(type, newVal);
                    src.sendMessage(Text.of("Set ", TextColors.AQUA, type.getName(), TextColors.RESET, " (", type.getName(), ") to ", TextColors.YELLOW, "EMC ", TextColors.RESET, newVal.toString(), Text.NEW_LINE,
                            "It's recommended to ", Text.builder("/rebuildEMC").style(TextStyles.UNDERLINE).color(TextColors.BLUE).onClick(TextActions.runCommand("/rebuildEMC")).build(), " after you're done editing."));

                    return CommandResult.success();
                }).build(), "setemc");
        //endregion
        //region /resetEMC
        Sponge.getCommandManager().register(EquivalentMatter.getInstance(), CommandSpec.builder()
                .permission("equmatex.command.resetemc")
                .description(Text.of("Set this item-types EMC value to be recalculated again."))
                .arguments(
                        GenericArguments.catalogedElement(Text.of("ItemType"), ItemType.class)
                ).executor((src,args)->{
                    if (Calculator.isCalculating())
                        throw new CommandException(Text.of("Calculator is currently refreshing values"));
                    ItemType type = args.<ItemType>getOne("ItemType").get();

                    Calculator.resetCost(type);
                    src.sendMessage(Text.of("Reset ", TextColors.AQUA, type.getName(), TextColors.RESET, " (", type.getName(), ").", Text.NEW_LINE,
                            "The value is no long fixed and will update the next time you ", Text.builder("/rebuildEMC").style(TextStyles.UNDERLINE).color(TextColors.BLUE).onClick(TextActions.runCommand("/rebuildEMC")).build(), "."));

                    return CommandResult.success();
                }).build(), "resetemc");
        //endregion
        //region /rebuildEMC
        Sponge.getCommandManager().register(EquivalentMatter.getInstance(), CommandSpec.builder()
                .permission("equmatex.command.rebuildemc")
                .description(Text.of("Rerun EMC value calculation. This will safe configs afterwards."))
                .arguments(
                        GenericArguments.none()
                ).executor((src,args)->{
                    if (Calculator.isCalculating())
                        throw new CommandException(Text.of("Calculator is currently refreshing values"));

                    EquivalentMatter.getInstance().invokeAsyncCalculation()
                            .whenCompleteAsync((v,e)->{
                                if (e != null) {
                                    try { src.sendMessage(Text.of(TextColors.RED, "EMC Calculation failed: ", e.getMessage())); } catch (Exception ignore) {}
                                    e.printStackTrace();
                                } else {
                                    try { src.sendMessage(Text.of(TextColors.GREEN, "EMC Calculation succeeded!")); } catch (Exception ignore) {}
                                }
                            });
                    src.sendMessage(Text.of(TextColors.YELLOW, "Please wait while the EMC-Values are recalculating."));

                    return CommandResult.success();
                }).build(), "rebuildemc");
        //endregion
        //region /reloadEMC
        Sponge.getCommandManager().register(EquivalentMatter.getInstance(), CommandSpec.builder()
                .permission("equmatex.command.reloademc")
                .description(Text.of("Reload all EMC values from config. This will not start a calculation!"))
                .arguments(
                        GenericArguments.none()
                ).executor((src,args)->{
                    if (Calculator.isCalculating())
                        throw new CommandException(Text.of("Calculator is currently refreshing values"));

                    EquivalentMatter.getInstance().loadConfigs();
                    src.sendMessage(Text.of(TextColors.GREEN, "EMC values reloaded successfully."));

                    return CommandResult.success();
                }).build(), "reloademc");
        //endregion
    }
}
