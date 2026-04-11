# Rainbow

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

Rainbow is a client-side Minecraft mod for the Fabric modloader to generate Geyser item mappings and bedrock resourcepacks
for use with Geyser's [custom item API (v2)](https://geysermc.org/wiki/geyser/custom-items). Rainbow is available for Minecraft 26.1.

Rainbow is currently experimental, and only capable of generating Geyser item mappings and bedrock resourcepacks for
somewhat simple 2D and 3D items. Features like animated textures are not yet supported. For a more descriptive list
of Rainbow's capabilities, see further below.

Rainbow works by detecting custom items in your inventory, or a container/inventory menu you have opened. It analyses
the components of detected items, and uses assets from loaded Java resourcepacks to gather information about item models, textures,
and more.

## Usage

You can download the latest version of Rainbow [here](https://download.geysermc.org/v2/projects/rainbow/versions/latest/builds/latest/downloads/rainbow). Using Rainbow requires the [Fabric API](https://modrinth.com/mod/fabric-api) to be installed. 

To use Rainbow, you must install it on your Minecraft client. Rainbow adds a few commands to the client. Generally,
you use them as follows:

1. First, start a new pack by running `/rainbow create <name>`, replacing `<name>` with the name of your pack. Your resourcepack and item mappings will be exported in the `.minecraft/rainbow/<name>` folder. Anything in here can be overwritten!
2. Once you have created a pack, you can start mapping custom items. Mapped custom items will be included in the exported resourcepack and item mappings. There are 3 ways to map custom items:
   - `/rainbow map` - maps the custom item you're currently holding in your hand, if any.
   - `/rainbow mapinventory` - scans your inventory for custom items, and maps all that are found.
   - `/rainbow auto inventory` - scans all inventory menus and containers you open for custom items, and maps all that are found. This is handy for plugins that offer an inventory menu listing all custom items. Use `/rainbow auto stop` to stop the mapping of custom items.
3. Once you have mapped all of your custom items, use `/rainbow finish` to finish the pack. Rainbow will then export the resourcepack and item mappings it has created.

When you've finished your pack, navigate to the `.minecraft/rainbow/<name>` folder. You can also click on the `Wrote pack to disk` in chat to open this folder.
In this folder, you'll find 3 important files:

- `geyser_mappings.json`: you need to put this file in the `custom_mappings` folder in Geyser's config folder.
- `pack.zip`: you need to put this file in the `packs` folder in Geyser's config folder.
- `report.txt`: you don't need to do anything with this file, but it contains information about generated assets and possible problems that occurred.

Once you have taken these steps, restart your server. Bedrock players should then download the generated pack upon joining,
and if everything went well, they should be able to see custom items!

If you have any questions or run into any problems, please do feel free to ask for support in the Geyser Discord!

## What Rainbow can do

Rainbow is currently capable of the following:

- Generating Geyser item mappings complete with data components and proper bedrock options, by detecting items with a custom `minecraft:item_model` component and analysing their components.
  - Also includes generating mappings with predicates for more complicated Java item model definitions, such as checks for if an item is broken. The following definition types are currently supported by Rainbow:
    - Plain item model definitions.
    - Conditional item models, supported properties are:
      - `broken`,
      - `damaged`,
      - `cutom_model_data`,
      - `has_component`, and,
      - `fishing_rod/cast`.
    - Range dispatch item models, supported properties are:
      - `bundle/fullness`,
      - `count`,
      - `custom_model_data`, and,
      - `damage`.
    - Select item models, supported properties are:
      - `charge_type`,
      - `trim_material`,
      - `context_dimension`, and,
      - `custom_model_data`.
      - For the `display_context` property, the `gui` case is mapped, if present.
  - Also includes detecting if an item should be displayed handheld by looking at the item's model.
  - Also is able to detect and map items using the "legacy" `custom_model_data` range-dispatch style, and map them to Geyser's `legacy` item mappings.
- Generating a simple bedrock resourcepack for simple 2D items, as well as:
  - Simple custom armour items, by analysing an item's `minecraft:equippable` component and loaded equipment assets.
  - 3D items, by converting the Java model to a bedrock one, and generating an attachable and animations for it, as well as rendering a custom GUI icon.
    - Is able to translate display transformations for the head, first-person and third-person item slots.
