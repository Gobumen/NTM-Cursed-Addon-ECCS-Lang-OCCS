package com.leafia.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.leafia.contents.AddonBlocks.Elevators;
import com.leafia.contents.AddonItems;
import com.leafia.contents.AddonItems.LeafiaRods;
import com.leafia.unsorted.ateupd.Reserved6TE;
import mezz.jei.api.ingredients.IIngredientBlacklist;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class _JEIBlacklist {
	public static void blacklistRecipes(IIngredientBlacklist blacklist) {
		for (EnumPWRFuel value : EnumPWRFuel.values()) {
			int i = value.ordinal();
			blacklist.addIngredientToBlacklist(new ItemStack(ModItems.pwr_fuel,1,i));
			blacklist.addIngredientToBlacklist(new ItemStack(ModItems.pwr_fuel_hot,1,i));
			blacklist.addIngredientToBlacklist(new ItemStack(ModItems.pwr_fuel_depleted,1,i));
		}
		for (Block block : Reserved6TE.ateupd)
			blacklist.addIngredientToBlacklist(new ItemStack(block));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_block));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_casing));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_channel));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_control));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_fuelrod));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_controller));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_port));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_reflector));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_neutron_source));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_heatex));
		blacklist.addIngredientToBlacklist(new ItemStack(ModBlocks.pwr_heatsink));
		blacklist.addIngredientToBlacklist(new ItemStack(AddonItems.billet_kys));
		blacklist.addIngredientToBlacklist(new ItemStack(LeafiaRods.leafRodKys));
		blacklist.addIngredientToBlacklist(new ItemStack(Elevators.light));
	}
}
