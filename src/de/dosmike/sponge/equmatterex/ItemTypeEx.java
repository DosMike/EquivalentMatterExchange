package de.dosmike.sponge.equmatterex;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.item.ArmorTypeProperty;
import org.spongepowered.api.data.property.item.ToolTypeProperty;
import org.spongepowered.api.data.property.item.UseLimitProperty;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemTypeEx {

    private static final DataQuery unsafeData = DataQuery.of("UnsafeDamage");
    private static final Pattern serial = Pattern.compile("^((?:[a-z][a-z0-9_]*[:])?[a-z][a-z0-9_]*)(?:[|]([0-9]+))?$");

    private ItemType type;
    private int damage;

    ItemTypeEx(ItemType itemType) {
        this(itemType, 0);
    }
    ItemTypeEx(ItemType itemType, int damage) {
        this.type = itemType;
        if (itemType.getDefaultProperty(ToolTypeProperty.class).isPresent() ||
            itemType.getDefaultProperty(ArmorTypeProperty.class).isPresent() ||
            itemType.getDefaultProperty(UseLimitProperty.class).isPresent() ||
            damage > 15) //backed potato recipe, wth? - item types never exceed 16 variants in vanilla
            this.damage = 0;
        else
            this.damage = damage;
    }

    public static ItemTypeEx of(ItemType itemType) {
        assert itemType!=null : "ItemType can't be null";
        return new ItemTypeEx(itemType, 0);
    }
    public static ItemTypeEx of(ItemType itemType, int damage) {
        assert itemType!=null : "ItemType can't be null";
        assert damage>=0 : "Damage can't be negative";
        return new ItemTypeEx(itemType, damage);
    }
    public static ItemTypeEx of(ItemStack stack) {
        assert stack!=null : "Stack can't be null";
        Integer dmg = stack.toContainer()
                .get(unsafeData)
                .map(o->(Integer)o)
                .orElse(0);
        return new ItemTypeEx(stack.getType(), dmg);
    }
    public static ItemTypeEx of(ItemStackSnapshot stack) {
        assert stack!=null : "Stack can't be null";
        Integer dmg = stack.toContainer()
                .get(unsafeData)
                .map(o->(Integer)o)
                .orElse(0);
        return new ItemTypeEx(stack.getType(), dmg);
    }

    public ItemStack itemStack() {
        return ItemStack.builder()
                .fromContainer(ItemStack.of(type, 1)
                        .toContainer()
                        .set(unsafeData, damage)
                )
                .build();
    }
    public ItemStack itemStack(int quantity) {
        return ItemStack.builder()
                .fromContainer(ItemStack.of(type, 1)
                        .toContainer()
                        .set(unsafeData, damage)
                )
                .quantity(quantity)
                .build();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ItemTypeEx)) return false;
        ItemTypeEx other = (ItemTypeEx)obj;
        return other.type.equals(type) && other.damage == damage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, damage);
    }

    public boolean isOfType(ItemStack stack) {
        return this.equals(of(stack));
    }
    public boolean isOfType(ItemStackSnapshot stack) {
        return this.equals(of(stack));
    }

    public ItemType getType() {
        return type;
    }
    public int getDamage() {
        return damage;
    }
    public String getId() {
        return String.format("%s|%d", type.getId(), damage);
    }
    @Override
    public String toString() {
        return getId();
    }
    public static Optional<ItemTypeEx> valueOf(String string) {
        try {
            Matcher m = serial.matcher(string);
            if (!m.matches()) return Optional.empty();
            String itemType = m.group(1);
            String damage = m.group(2);
//            EquivalentMatter.l("Builting ItemTypeEx: %s %s", itemType, damage);
            return Optional.of(of(
                    Sponge.getRegistry().getType(ItemType.class, itemType).get(),
                    damage == null ? 0 : Integer.parseInt(damage)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
