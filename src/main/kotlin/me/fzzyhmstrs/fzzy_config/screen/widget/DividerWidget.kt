package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.fcId
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

@Environment(EnvType.CLIENT)
class DividerWidget(width: Int): Widget, Drawable, Scalable {
    private var xx = 0
    private var yy = 0
    private var ww = width
    private var hh = 1

    override fun setX(x: Int) {
        this.xx = x
    }
    override fun getX(): Int {
        return xx
    }

    override fun setY(y: Int) {
        this.yy = y
    }
    override fun getY(): Int {
        return yy
    }

    override fun getWidth(): Int {
        return ww
    }
    override fun setWidth(width: Int) {
        ww = width
    }

    override fun getHeight(): Int {
        return hh
    }
    override fun setHeight(height: Int) {
        hh = height
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>) {
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.drawGuiTexture(DIVIDER,xx,yy - 3,ww,hh + 6)
    }

    companion object{
        private val DIVIDER = "widget/popup/divider".fcId()
    }


}