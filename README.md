# EquivalentMatterExchange
A ProjectE like plugin for Sponge and vanilla Minecraft Clients

This plugin features a limited set of Features from the ProjectE mod, including:
* Energy Condenser: Being able to convert one item into another by putting the 
resources into a special chest
* Energy Collectors: Havest EMC with the energy of light
* Transmutation Tables: Store EMC on yourself by feeding items into this, and 
pull out anything you'd like at a different time.
* EMC Value calculation: The plugin itterates through all crafting and smelting 
recipes accessible through the means of the Sponge API to automatically resolve 
the potential value of each item in Minecraft.

### How to build the Devices:
Simply put a daylight detector on top of the base block.
The base block for a Condenser is the chest, for a Collector its the furnace and
for Transmutation Tables it's the workbench.

To specify a target for the Condenser just put a ItemFrame in from of the chest's
lid and put the target item in it.

Condensers and Collectors can be upgraded by infusing the daylight sensor with an
end crystal. This will drastically improve the devices function.

If you want to hide the hologram that's displaying device information, you can 
use-click it while sneaking to toggle the hologram on and off.

As of writing this there is no posibillity to limit device creation, as they do
not store owner information.