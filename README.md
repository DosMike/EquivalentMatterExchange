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

### Permissions and commands

| Permission   | Description    |
|----- |----- |
| equmatex.command.emc  | /emc Check the EMC value of an item-type  |
| equmatex.command.setemc  | /setemc Fix the EMC value for some item, this may cause inconsistencies in recipe calculations!  |
| equmatex.command.resetemc  | /resetemc Set this item-types EMC value to be recalculated again.  |
| equmatex.command.rebuildemc  | /rebuildemc Rerun EMC value calculation. This will safe configs afterwards.  |
| equmatex.command.reloademc  | /reloademc Reload all EMC values from config. This will not start a calculation!  |
| equmatterex.device.collector.create  | Allows a user to create a Energy Collector  |
| equmatterex.device.collector.sharedaccess  | Allows a user access and destroy Energy Collectors from other players  |
| equmatterex.device.condenser.create  | Allows a user to create a Energy Condenser  |
| equmatterex.device.condenser.sharedaccess  | Allows a user access and destroy Energy Condenser from other players  |
| equmatterex.device.transmutation_table.create  | Allows a user to create a Transmutation Table  |
| equmatterex.device.transmutation_table.sharedaccess  | Allows a user access and destroy Transmutation Table from other players  |

The device.conf allows you to specify whether a device permission is actually used or not.