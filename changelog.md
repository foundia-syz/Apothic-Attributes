## 2.4.0
* `IFormattableAttribute`, `GatherSkippedAttributeTooltipsEvent`, and `AddAttributeTooltipsEvent` have all been removed.
  * These were added to NeoForge in 21.1.60.
* Apothic Attributes enables NeoForge attribute tooltip merging.

## 2.3.0
* Updated to Minecraft 1.21.1.
* Removed AA's internal `BooleanAttribute` in favor of the one provided by NeoForge.
  * A mixin was added to preserve the attribute formatting behavior from AA's original.
* Fixed the config directory being named `apothic_attributes` instead of `apotheosis`.
  * This will invalidate some existing config files, so please move your originals appropriately.
* Added a config option for Ancient Knowledge's experience gain value.

## 2.2.2
* Updated to Placebo 9.3.5.

## 2.2.1
* Fixed the random seed selection for Dodge Chance to be properly random.
  * The prior seed selection would cause non-uniform values to be selected, causing players to dodge or not dodge for long periods of time.
* Added support for display of Dynamic Attributes (which are more like stacks of modifiers to be applied to some value later in time) in the attributes GUI.
  * These attributes will need to be tagged as `apothic_attributes:dynamic_base` to be picked up.
* Added the full modifier formula to the displayed elements in the attributes GUI.

## 2.2.0
* Updated to Minecraft 1.21.

## 2.1.2
* Removed leftover code from the transition to neo's flying attribute.
* Repackaged `.mobfx` to `.mob_effect`.
* Updated potion tooltip formatting.

## 2.1.1
* Fixed some things that missed the modid transition.
* Fixed tridents not respecting the arrow damage attribute.

## 2.1.0
* Changed the modid from `attributeslib` to `apothic_attributes`.

## 2.0.1
* Made armor / protection formulas configurable.

## 2.0.0
* Updated to 1.20.4 (NeoForge).
* Pulled all Potions that were previously in the Potion Module of Apotheosis.
  * The recipes are also present, but can now be edited via datapack without external tools.

## 1.3.1
* Added a null check to the code that fires the attribute changed event.

## 1.3.0
* Added a config file.
  * Added an option to disable the Attributes GUI.
  * Added an option to disable potion item tooltips.
  * Added an option to hide specific attributes from the GUI.
* Fixed a bug causing sweep attacks to not trigger if auxiliary damage (fire/cold/%hp) killed the target.
* Fixed the dodge sound not being registered.
* Fixed an issue where the GUI button could become stuck in the focused state.
* BookerCatch: Made the Attributes GUI accessible from the Curios menu.
* mc-kaishixiaxue: Updated Chinese translation.
* t0piy: Updated Brazilian translation.

## 1.2.1
* Fixed the flying state being reset when changing dimensions.

## 1.2.0
* ChampionAsh: Removed the entity gravity modifier from Slow Fall
  * This was causing high levels of slow fall to make people skyrocket into the air, and was also increasing jump height.
  * This means the gravity modifier from slow fall will no longer be tracked in the attributes gui.
* ChampionAsh: Made `AttributeChangedValueEvent` only execute once for batched attribute updates (such as bulk removals + reapplications).
* Fixed the lang key `attributeslib.gui.current` being unused.

## 1.1.4
* Fixed players losing the ability to fly when switching between Creative mode and Spectator mode.

## 1.1.3
* Fixed typo in `GameTypeMixin` causing players to still lose attribute-provided flight when switching back from Creative to Survival.

## 1.1.2
* Fixed the Flight effect not working on relog or when switching game modes.

## 1.1.1
* No longer modifies the flying state when the player is in spectator mode.

## 1.1.0
* Added support for Boolean attributes, which only have an Enabled or Disabled state.
  * A boolean attribute is enabled if the current value is > 0, and disabled if the value == 0.
  * Only two modifiers will display properly for boolean attributes:
    * An Addition +1 modifier will display as "Enables X"
    * A Multiply Total -1 modifier will display as "Forcibly Disables X"
    * Other modifiers will display as "Invalid X", and should not be used for Boolean attributes.
* Added the Elytra Flight attribute.
* Added the Creative Flight attribute.
* Added the Flying mob effect, which grants Creative Flight.
* Made Fall Damage scale with Gravity. If your Gravity is below 0.01 (the amount slow falling reduces it to), fall damage is prevented entirely.

## 1.0.4
* Removed forge dependency line from the mods.toml and marked as Forge and NeoForge for CF.
  * The dependency will be added back and the Forge marker will be removed once CF supports Neo correctly.

## 1.0.3
* Made description convention use `Attribute#getDescriptionId` as the base instead of the registry name.
  * Description keys will now be equal to the normal key with `.desc` appended in all cases.

## 1.0.2
* Updated armor computations so damage values < 20 are much closer to vanilla.
  * Old Graph: https://i.imgur.com/3yEnTyi.png
  * New Graph: https://i.imgur.com/2OHQhgp.png

## 1.0.1
* Added back the code which updates the base value of step height on login.

## 1.0.0
* Initial Release