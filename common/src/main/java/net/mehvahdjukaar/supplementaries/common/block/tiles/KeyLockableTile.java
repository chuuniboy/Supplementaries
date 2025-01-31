package net.mehvahdjukaar.supplementaries.common.block.tiles;

import net.mehvahdjukaar.supplementaries.common.utils.ItemsUtil;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class KeyLockableTile extends BlockEntity {

    private String password = null;

    public KeyLockableTile(BlockPos pos, BlockState state) {
        super(ModRegistry.KEY_LOCKABLE_TILE.get(), pos, state);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPassword(ItemStack stack) {
        this.setPassword(stack.getHoverName().getString());
    }

    public void clearOwner() {
        this.password = null;
    }

    public static boolean isCorrectKey(ItemStack key, String password) {
        return key.getHoverName().getString().equals(password);
    }

    public boolean isCorrectKey(ItemStack key) {
        return isCorrectKey(key, this.password);
    }

    public enum KeyStatus {
        CORRECT_KEY,
        INCORRECT_KEY,
        NO_KEY
    }

    public static boolean doesPlayerHaveKeyToOpen(Player player, String lockPassword, boolean feedbackMessage, @Nullable String translName) {
        KeyStatus key = ItemsUtil.hasKeyInInventory(player, lockPassword);
        if (key == KeyStatus.INCORRECT_KEY) {
            if (feedbackMessage)
                player.displayClientMessage(Component.translatable("message.supplementaries.safe.incorrect_key"), true);
            return false;
        } else if (key == KeyStatus.CORRECT_KEY) return true;
        if (feedbackMessage)
            player.displayClientMessage(Component.translatable("message.supplementaries." + translName + ".locked"), true);
        return false;
    }


    //returns true if door has to open
    public boolean handleAction(Player player, InteractionHand handIn, String translName) {
        if (player.isSpectator()) return false;
        ItemStack stack = player.getItemInHand(handIn);

        boolean isKey = stack.is(ModTags.KEY);
        //clear ownership
        if (player.isSecondaryUseActive() && isKey) {
            if (tryClearingKey(player, stack)) return false;
        }
        //set key
        else if (this.password == null) {
            if (isKey) {
                this.setPassword(stack);
                player.displayClientMessage(Component.translatable("message.supplementaries.safe.assigned_key", this.password), true);
                this.level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.5F);
                return false;
            }
            return true;
        }
        //open
        return player.isCreative() || doesPlayerHaveKeyToOpen(player, this.password, true, translName);
    }

    public boolean tryClearingKey(Player player, ItemStack stack) {
        if ((player.isCreative() || this.isCorrectKey(stack))) {
            this.clearOwner();
            player.displayClientMessage(Component.translatable("message.supplementaries.safe.cleared"), true);
            this.level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.5F);
            return true;
        }
        return false;
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (compound.contains("Password")) {
            this.password = compound.getString("Password");
        } else this.password = null;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.password != null) {
            tag.putString("Password", this.password);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
