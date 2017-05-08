/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.DynSurround.client.handlers.scanners;

import java.util.List;

import javax.annotation.Nonnull;

import org.blockartistry.DynSurround.client.handlers.EnvironStateHandler.EnvironState;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BattleScanner implements ITickable {

	private static final int BOSS_RANGE = 65536; // 256 block range
	private static final int MINI_BOSS_RANGE = 16384; // 128 block range
	private static final int MOB_RANGE = 256; // 16 block range

	protected boolean inBattle;
	protected boolean isWither;
	protected boolean isDragon;
	protected boolean isBoss;

	protected void reset() {
		this.inBattle = false;
		this.isWither = false;
		this.isDragon = false;
		this.isBoss = false;
	}

	public boolean inBattle() {
		return this.inBattle;
	}

	public boolean isWither() {
		return this.isWither;
	}

	public boolean isDragon() {
		return this.isDragon;
	}

	public boolean isBoss() {
		return this.isBoss;
	}

	@Override
	public void update() {

		this.reset();

		final BlockPos playerPos = EnvironState.getPlayerPosition();
		final World world = EnvironState.getWorld();

		// Find all the possible candidates that influence battle flags
		final Predicate<Entity> hostileFilter = new Predicate<Entity>() {
			@Override
			public boolean apply(@Nonnull final Entity e) {
				final double dist = e.getDistanceSq(playerPos);
				if (dist <= BOSS_RANGE && (e instanceof EntityWither || e instanceof EntityDragon))
					return true;
				if (dist <= MINI_BOSS_RANGE && !e.isNonBoss())
					return true;
				if (dist <= MOB_RANGE && e.isNonBoss() && e instanceof EntityMob) {
					if (e instanceof EntityPigZombie) {
						return ((EntityPigZombie) e).isAngry();
					} else if (e instanceof EntityEnderman) {
						return ((EntityEnderman) e).isScreaming();
					}
					return true;
				}
				return false;
			}
		};

		// If nothing matches return
		final List<Entity> candidates = world.getEntities(Entity.class, hostileFilter);
		if (candidates.isEmpty())
			return;

		// Battle flag is set regardless of hostile discovered
		this.inBattle = true;

		// Rip through looking for withers, dragons, and mini bosses
		for (final Entity e : candidates)
			if (e instanceof EntityWither)
				this.isWither = true;
			else if (e instanceof EntityDragon)
				this.isDragon = true;
			else if (!e.isNonBoss())
				this.isBoss = true;

		// Make sure the boss flag is set based on wither/dragon
		this.isBoss = this.isBoss || this.isWither || this.isDragon;

		// Wither trumps dragon
		if (this.isWither)
			this.isDragon = false;

		// Flags should be set appropriately by now
	}

}
