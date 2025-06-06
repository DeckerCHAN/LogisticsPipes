package logisticspipes.proxy.forestry;

import java.lang.reflect.Method;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.core.ForestryAPI;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.recipes.RecipeManagers;
import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemPipeComponents;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.interfaces.ICraftingParts;
import logisticspipes.proxy.interfaces.IForestryProxy;
import logisticspipes.recipes.CraftingDependency;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.RecipeManager.LocalCraftingManager;
import logisticspipes.utils.item.ItemIdentifier;

public class ForestryProxy implements IForestryProxy {

    public ForestryProxy() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        analyserClass = Class.forName("forestry.core.tiles.TileAnalyzer");
        Class<?> stringUtil = Class.forName("forestry.core.utils.StringUtil");
        localize = stringUtil.getDeclaredMethod("localize", String.class);
        localize.setAccessible(true);
        propolis = GameRegistry.findItem("Forestry", "propolis");
        pollen = GameRegistry.findItem("Forestry", "pollen");
        honey = FluidRegistry.getFluidStack("for.honey", 1500);
        root = (IBeeRoot) AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
    }

    private final Class<?> analyserClass;
    private final Method localize;
    private final Item propolis;
    private final Item pollen;
    private final FluidStack honey;
    private final IBeeRoot root;

    /**
     * Checks if item is bee via ItemIdentifier.
     *
     * @param item ItemIdentifier to check if is bee.
     * @return Boolean, true if item is bee.
     */
    @Override
    public boolean isBee(ItemIdentifier item) {
        return isBee(item.unsafeMakeNormalStack(1));
    }

    /**
     * Checks if item is bee.
     *
     * @param item ItemStack to check if is bee.
     * @return Boolean, true if item is bee.
     */
    @Override
    public boolean isBee(ItemStack item) {
        return root.isMember(item);
    }

    /**
     * First checks if item is bee, then returns boolean if its analyzed. Then it will check if its analyzed.
     *
     * @param item ItemIdentifier to check if is analyzed bee.
     * @return Boolean, true if item is analyzed bee.
     */
    @Override
    public boolean isAnalysedBee(ItemIdentifier item) {
        return isAnalysedBee(item.unsafeMakeNormalStack(1));
    }

    /**
     * First checks if item is bee, then checks if its analyzed.
     *
     * @param item ItemStack to check if is analyzed bee.
     * @return Boolean, true if item is analyzed bee.
     */
    @Override
    public boolean isAnalysedBee(ItemStack item) {
        if (!isBee(item)) {
            return false;
        }
        return root.getMember(item).isAnalyzed();
    }

    /**
     * Checks if a passed tile entity is a Forestry Analyzer.
     *
     * @param tile The TileEntity to check if is Forestry Analyzer.
     * @return Boolean, true if tile is a Forestry Analyzer.
     */
    @Override
    public boolean isTileAnalyser(TileEntity tile) {
        try {
            if (analyserClass.isAssignableFrom(tile.getClass())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if passed string allele was discovered by the player in passed world.
     *
     * @param allele The allele as a String.
     * @param world  The world to check in.
     * @return Boolean, true if allele was discovered in world.
     */
    @Override
    public boolean isKnownAlleleId(String allele, World world) {
        if (!(forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(allele) instanceof IAlleleBeeSpecies)) {
            return false;
        }
        if (!((IAlleleSpecies) forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(allele)).isSecret()) {
            return true;
        }
        return root.getBreedingTracker(world, MainProxy.proxy.getClientPlayer().getGameProfile())
                .isDiscovered((IAlleleSpecies) forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(allele));
    }

    /**
     * Returns a String for the uid passed for allele name.
     *
     * @param uid The uid as string to get proper name for.
     * @return String of the actual user-friendly name for the allele.
     */
    @Override
    public String getAlleleName(String uid) {
        if (!(forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(uid) instanceof IAlleleSpecies)) {
            return "";
        }
        return AlleleManager.alleleRegistry.getAllele(uid).getName();
    }

    /**
     * Returns the first valid allele uid as String.
     *
     * @param world The world to check in.
     * @return The first valid allele as uid.
     */
    private String getFirstValidAllele(World world) {
        for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values()) {
            if (allele instanceof IAlleleBeeSpecies && isKnownAlleleId(allele.getUID(), world)) {
                return allele.getUID();
            }
        }
        return "";
    }

    /**
     * Returns the last valid allele uid as String.
     *
     * @param world The world to check in.
     * @return The last valid allele as uid.
     */
    private String getLastValidAllele(World world) {
        String uid = "";
        for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values()) {
            if (allele instanceof IAlleleBeeSpecies && isKnownAlleleId(allele.getUID(), world)) {
                uid = allele.getUID();
            }
        }
        return uid;
    }

    /**
     * Returns a String of a uid after the one passed in.
     *
     * @param uid   The uid used as a reference.
     * @param world The world to check in.
     * @return String of uid after the one passed in.
     */
    @Override
    public String getNextAlleleId(String uid, World world) {
        if (!(forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(uid) instanceof IAlleleBeeSpecies)) {
            return getFirstValidAllele(world);
        }
        boolean next = false;
        for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values()) {
            if (allele instanceof IAlleleBeeSpecies) {
                if (next && isKnownAlleleId(allele.getUID(), world)) {
                    return allele.getUID();
                } else if (allele.getUID().equals(uid)) {
                    next = true;
                }
            }
        }
        return "";
    }

    /**
     * Returns a String of a uid before the one passed in.
     *
     * @param uid The uid used as a reference.
     * @return String of uid before the one passed in.
     */
    @Override
    public String getPrevAlleleId(String uid, World world) {
        if (!(forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(uid) instanceof IAlleleBeeSpecies)) {
            return getLastValidAllele(world);
        }
        IAllele lastAllele = null;
        for (IAllele allele : AlleleManager.alleleRegistry.getRegisteredAlleles().values()) {
            if (allele instanceof IAlleleBeeSpecies) {
                if (allele.getUID().equals(uid)) {
                    if (lastAllele == null) {
                        return "";
                    }
                    return lastAllele.getUID();
                } else if (isKnownAlleleId(allele.getUID(), world)) {
                    lastAllele = allele;
                }
            }
        }
        return "";
    }

    /**
     * Checks if passed ItemStack is bee, then returns its first allele.
     *
     * @param bee the ItemStack to get the first allele for.
     * @return String of the first allele of bee.
     */
    @Override
    public String getFirstAlleleId(ItemStack bee) {
        if (!isBee(bee)) {
            return "";
        }
        return root.getMember(bee).getGenome().getPrimary().getUID();
    }

    /**
     * Checks if passed ItemStack is bee, then returns its second allele.
     *
     * @param bee the ItemStack to get the second allele for.
     * @return String of the second allele of bee.
     */
    @Override
    public String getSecondAlleleId(ItemStack bee) {
        if (!isBee(bee)) {
            return "";
        }
        return root.getMember(bee).getGenome().getSecondary().getUID();
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a drone.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a drone.
     */
    @Override
    public boolean isDrone(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.isDrone(bee);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a princess.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is princess.
     */
    @Override
    public boolean isPrincess(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        if (isQueen(bee)) {
            return false;
        }
        return !isDrone(bee);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a queen.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is queen.
     */
    @Override
    public boolean isQueen(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.isMated(bee);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a purebred.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a purebred bee.
     */
    @Override
    public boolean isPurebred(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).isPureBred(EnumBeeChromosome.SPECIES);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its nocturnal.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a nocturnal bee.
     */
    @Override
    public boolean isNocturnal(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getNocturnal();
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a purebred nocturnal.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a purebred nocturnal bee.
     */
    @Override
    public boolean isPureNocturnal(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getNocturnal()
                && root.getMember(bee).isPureBred(EnumBeeChromosome.NOCTURNAL);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a tolerant flyer.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a tolerant flyer bee.
     */
    @Override
    public boolean isFlyer(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getTolerantFlyer();
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a purebred tolerant flyer.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a purebred tolerant flyer bee.
     */
    @Override
    public boolean isPureFlyer(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getTolerantFlyer()
                && root.getMember(bee).isPureBred(EnumBeeChromosome.TOLERANT_FLYER);
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a cave dweller.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a cave dweller bee.
     */
    @Override
    public boolean isCave(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getCaveDwelling();
    }

    /**
     * Checks if passed ItemStack is bee, then checks if its a purebred cave dweller.
     *
     * @param bee The ItemStack to check.
     * @return Boolean, true if passed ItemStack is a purebred cave dweller bee.
     */
    @Override
    public boolean isPureCave(ItemStack bee) {
        if (!isBee(bee)) {
            return false;
        }
        return root.getMember(bee).getGenome().getCaveDwelling()
                && root.getMember(bee).isPureBred(EnumBeeChromosome.CAVE_DWELLING);
    }

    /**
     * Returns a special Forestry translation of the passed String.
     *
     * @param input The String to translate.
     * @return The translated string.
     */
    @Override
    public String getForestryTranslation(String input) {
        try {
            return (String) localize.invoke(null, input.toLowerCase(Locale.US));
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    /**
     * Void method, called to initialize LogisticsPipes' Forestry recipes.
     */
    @Override
    public void addCraftingRecipes(ICraftingParts parts) {
        if (LogisticsPipes.isGTNH) return;
        // Enable the Beta-Recipes
        if (Configs.ENABLE_BETA_RECIPES) {
            // Enable Carpenter-based Recipes
            if (Configs.MANDATORY_CARPENTER_RECIPES) {
                ItemStack packager = new ItemStack(
                        LogisticsPipes.LogisticsPipeComponents,
                        1,
                        ItemPipeComponents.ITEM_MICROPACKAGER);
                ItemStack expand = new ItemStack(
                        LogisticsPipes.LogisticsPipeComponents,
                        1,
                        ItemPipeComponents.ITEM_LOGICEXPANDER);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        packager,
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEESINK),
                        "CrC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTREFILLER),
                        " p ",
                        "r r",
                        "CwC",
                        'p',
                        pollen,
                        'C',
                        propolis,
                        'w',
                        new ItemStack(
                                LogisticsPipes.LogisticsPipeComponents,
                                1,
                                ItemPipeComponents.ITEM_MICROCAPSULATOR),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTTERMINUS),
                        "CGD",
                        "s s",
                        "DrC",
                        'C',
                        "dyeBlack",
                        'D',
                        "dyePurple",
                        'G',
                        pollen,
                        'r',
                        expand,
                        's',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0),
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        expand,
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0),
                        new ItemStack(LogisticsPipes.LogisticsApiaristSinkPipe, 1, 0),
                        "CrC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone);
            }
            // Disable Carpenter-based Recipes
            if (!Configs.MANDATORY_CARPENTER_RECIPES) {
                LocalCraftingManager manager = RecipeManager.craftingManager;
                ItemStack packager = new ItemStack(
                        LogisticsPipes.LogisticsPipeComponents,
                        1,
                        ItemPipeComponents.ITEM_MICROPACKAGER);
                ItemStack expand = new ItemStack(
                        LogisticsPipes.LogisticsPipeComponents,
                        1,
                        ItemPipeComponents.ITEM_LOGICEXPANDER);

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        packager,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        expand,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEESINK),
                        CraftingDependency.High_Tech_Modules,
                        "CrC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.ITEMSINK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTREFILLER),
                        CraftingDependency.High_Tech_Modules,
                        " p ",
                        "rBr",
                        "CwC",
                        'p',
                        pollen,
                        'C',
                        propolis,
                        'w',
                        new ItemStack(LogisticsPipes.LogisticsPipeComponents, 1, ItemPipeComponents.ITEM_MICROSERVO),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTTERMINUS),
                        CraftingDependency.High_Tech_Modules,
                        "CGD",
                        "rBr",
                        "DrC",
                        'C',
                        "dyeBlack",
                        'D',
                        "dyePurple",
                        'G',
                        pollen,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        new ItemStack(
                                LogisticsPipes.LogisticsPipeComponents,
                                1,
                                ItemPipeComponents.ITEM_ROUTEPROCESSOR),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.LogisticsApiaristSinkPipe, 1, 0),
                        CraftingDependency.High_Tech_Modules,
                        "CrC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0));
            }
        }
        // Disable Beta-Recipes
        if (!Configs.ENABLE_BETA_RECIPES) {
            // Enable Carpenter-based Recipes
            if (Configs.MANDATORY_CARPENTER_RECIPES) {

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getGearTear1(),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getChipTear1(),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEESINK),
                        "CrC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTREFILLER),
                        " p ",
                        "r r",
                        "CwC",
                        'p',
                        pollen,
                        'C',
                        propolis,
                        'w',
                        parts.getExtractorItem(),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK),
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTTERMINUS),
                        "CGD",
                        "r r",
                        "DrC",
                        'C',
                        "dyeBlack",
                        'D',
                        "dyePurple",
                        'G',
                        pollen,
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0),
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getGearTear1(),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0),
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        "CGC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getChipTear1(),
                        'r',
                        Items.redstone);

                RecipeManagers.carpenterManager.addRecipe(
                        25,
                        honey,
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0),
                        new ItemStack(LogisticsPipes.LogisticsApiaristSinkPipe, 1, 0),
                        "CrC",
                        "r r",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone);
            }
            // Disable Carpenter-based Recipes
            if (!Configs.MANDATORY_CARPENTER_RECIPES) {
                LocalCraftingManager manager = RecipeManager.craftingManager;

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getGearTear1(),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEEANALYZER),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getChipTear1(),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BEESINK),
                        CraftingDependency.High_Tech_Modules,
                        "CrC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.ITEMSINK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTREFILLER),
                        CraftingDependency.High_Tech_Modules,
                        " p ",
                        "rBr",
                        "CwC",
                        'p',
                        pollen,
                        'C',
                        propolis,
                        'w',
                        parts.getExtractorItem(),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.APIARISTTERMINUS),
                        CraftingDependency.High_Tech_Modules,
                        "CGD",
                        "rBr",
                        "DrC",
                        'C',
                        "dyeBlack",
                        'D',
                        "dyePurple",
                        'G',
                        pollen,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.ModuleItem, 1, ItemModule.BLANK));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getGearTear1(),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.LogisticsApiaristAnalyzerPipe, 1, 0),
                        CraftingDependency.High_Tech_Modules,
                        "CGC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'G',
                        parts.getChipTear1(),
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0));

                manager.addRecipe(
                        new ItemStack(LogisticsPipes.LogisticsApiaristSinkPipe, 1, 0),
                        CraftingDependency.High_Tech_Modules,
                        "CrC",
                        "rBr",
                        "CrC",
                        'C',
                        propolis,
                        'r',
                        Items.redstone,
                        'B',
                        new ItemStack(LogisticsPipes.LogisticsBasicPipe, 1, 0));
            }
        }
    }

    /**
     * Used to get an icon index for a given allele.
     *
     * @param uid   The uid String of the allele to get icon index for.
     * @param phase special phase of the bee.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndexForAlleleId(String uid, int phase) {
        IAllele bSpecies = forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(uid);
        if (!(bSpecies instanceof IAlleleBeeSpecies)) {
            bSpecies = root.getDefaultTemplate()[forestry.api.apiculture.EnumBeeChromosome.SPECIES.ordinal()];
        }
        IAlleleBeeSpecies species = (IAlleleBeeSpecies) bSpecies;
        return species.getIcon(EnumBeeType.DRONE, phase);
    }

    /**
     * Used to get an color as int for a given allele.
     *
     * @param uid   The uid String of the allele to get color for.
     * @param phase special phase of the bee.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public int getColorForAlleleId(String uid, int phase) {
        if (!(forestry.api.genetics.AlleleManager.alleleRegistry.getAllele(uid) instanceof IAlleleBeeSpecies)) {
            return 16777215;
        }
        IAlleleBeeSpecies species = (IAlleleBeeSpecies) forestry.api.genetics.AlleleManager.alleleRegistry
                .getAllele(uid);
        return species.getIconColour(phase);
    }

    /**
     * Returns the number of render passes for given allele.
     *
     * @param uid The uid of the allele.
     * @return The number of render passes for the allele.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderPassesForAlleleId(String uid) {
        return 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromTextureManager(String name) {
        return ForestryAPI.textureManager.getDefault(name);
    }

    @Override
    public void syncTracker(World world, EntityPlayer player) {
        root.getBreedingTracker(world, player.getGameProfile()).synchToPlayer(player);
    }
}
