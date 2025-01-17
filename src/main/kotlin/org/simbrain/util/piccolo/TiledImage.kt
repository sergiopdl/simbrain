package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import org.simbrain.world.odorworld.OdorWorldResourceManager
import java.awt.image.BufferedImage

/**
 * An image containing multiple tile images. [source] is the path to this master image.
 */
@XStreamAlias("image")
class TiledImage (@XStreamAsAttribute private val source: String) {

    /**
     * Used for embedded images, in combination with a data child element. Not yet supported.
     */
    @XStreamAsAttribute
    private val format = ""

    /**
     * The image width in pixels (optional).
     */
    @XStreamAsAttribute
    private val width = 0

    /**
     * The image height in pixels (optional)
     */
    @XStreamAsAttribute
    private val height = 0

    /**
     * The tileset image
     */
    @Transient
    var image: BufferedImage? = null
        get() {
            if (field == null) {
                if (source.isNotEmpty()) {
                    val segments = source.split("/").toTypedArray()
                    field = OdorWorldResourceManager.getBufferedImage("tilemap/" + segments[segments.size - 1])
                }
            }
            return field
        }
        private set

}