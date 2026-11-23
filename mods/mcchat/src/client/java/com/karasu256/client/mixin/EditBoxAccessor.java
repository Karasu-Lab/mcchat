package com.karasu256.client.mixin;

import java.util.function.BiFunction;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
  @Accessor("formatter")
  BiFunction<String, Integer, FormattedCharSequence> chatnative$formatter();
}
