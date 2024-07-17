# Create Trains and Custom Portal API Compability

The mod provides compability between [Create](https://www.curseforge.com/minecraft/mc-mods/create) trains and portals 
created using the [Custom Portal API](https://www.curseforge.com/minecraft/mc-mods/custom-portal-api-forge). 

## Usage

### For Mod-Users

After installation, you can test if the track now connects through the portal. Make sure the portal is at least 3 blocks
wide in both dimensions and that it is linked correctly to the target portal and back. To do so, go through the portal 
and back. You can try laying track if you're back in the portal you started at. If not, you'll need to relink you're 
portals by breaking and reconstructing them.

If all this hasn't helped, you're portal is either not produced using the custom portal api, or my mod still has some
bugs (which is entirely possible). Please let me know in a comment about your use case, including the type of portal 
you wanted to lay track through and the mod responsible for adding this portal, and I'll look into it when I have the time. 

### For Mod-Authors

You don't need to do anything special to register you're custom portal with this addon after registering them in the 
Custom Portal API. The mod automatically reads the `PortalLink` from the registry and create a custom 
`PortalTrackProvider` for each of them. Just make sure to have you're portals registered by the end of
`FMLCommonSetupEvent`, as this mod enqueues it's work here. Any portals added later than this event won't work. 


