/*
 *  Copyright 2019 Nicholas Bennett & Deep Dive Coding/CNM Ingenuity
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.rps.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import edu.cnm.deepdive.rps.model.Arena

/**
 * [View] subclass that renders the cells on the terrain of an [Arena]. Note that this
 * is not a [android.view.SurfaceView]; updates are being driven entirely by updates to the
 * underlying LiveData; if these occur too frequently, it would probably make sense to implement a
 * [android.view.SurfaceView] subclass instead.
 *
 * @author Nicholas Bennett
 */
class TerrainView : View {
    private var canvas: Canvas? = null
    private var bitmap: Bitmap? = null
    private var arena: Arena? = null
    private var terrain: Array<ByteArray?>? = null
    private var breedColors: IntArray = intArrayOf()
    private var paint: Paint? = null
    private var measured = false
    private var generation: Long = 0

    /**
     * Initializes by chaining to [View.View].
     *
     * @param context
     */
    constructor(context: Context?) : super(context) {}

    /**
     * Initializes by chaining to [View.View].
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    /**
     * Initializes by chaining to [View.View].
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    /**
     * Initializes by chaining to [View.View].
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    /**
     * Returns dimensions based on the larger of this view's suggested height and width, so that the
     * content is square. For this to be the appropriate choice, this view should be contained within
     * a [android.widget.ScrollView], with its width set to `match_parent` and its height
     * set to `wrap_content`; or within a [android.widget.HorizontalScrollView], with its
     * width set to `wrap_content` and its height set to `match_parent`.
     *
     * @param widthMeasureSpec specification control value.
     * @param heightMeasureSpec specification control value.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measured = false
        var width = suggestedMinimumWidth
        var height = suggestedMinimumHeight
        width = resolveSizeAndState(paddingLeft + paddingRight + width, widthMeasureSpec, 0)
        height = resolveSizeAndState(paddingTop + paddingBottom + height, heightMeasureSpec, 0)
        val size = Math.max(width, height)
        setMeasuredDimension(size, size)
        bitmap = null
    }

    /**
     * Performs layout on all child elements (none), and creates a [Bitmap] to fit the specified
     * dimensions.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        measured = true
        updateBitmap()
    }

    /**
     * Renders the contents of the [Arena&#39;s][Arena] terrain.
     *
     * @param canvas rendering target.
     */
    override fun onDraw(canvas: Canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, 0f, 0f, null)
        }
    }

    /**
     * Specifices the [Arena] instance to be rendered by this view. In general, this will most
     * simply be invoked via data binding in the layout XML.
     *
     * @param arena instance to render.
     */
    fun setArena(arena: Arena?) {
        this.arena = arena
        if (arena != null) {
            val numBreeds = arena.numBreeds.toInt()
            val size = arena.arenaSize
            terrain = Array(size) { ByteArray(size) }
            val hsv = floatArrayOf(0f, SATURATION, BRIGHTNESS)
            val hueInterval = MAX_HUE / numBreeds
            breedColors = IntArray(numBreeds)
            for (i in 0 until numBreeds) {
                breedColors[i] = Color.HSVToColor(hsv)
                hsv[0] += hueInterval
            }
        }
    }

    /**
     * Updates the current generation count, triggering a display refresh. Without invoking this
     * method, the cell terrain rendering will not be updated; however, if data binding is used in the
     * layout XML, this can happen automatically.
     *
     * @param generation number of generations (iterations) completed in the [Arena] simulation.
     */
    fun setGeneration(generation: Long) {
        if (generation == 0L || generation != this.generation) {
            Thread(Runnable {
                updateBitmap()
                this.generation = generation
            }).start()
        }
    }

    private fun updateBitmap() {
        if (measured) {
            terrain?.let { terr ->
                if (bitmap == null) {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    canvas = bitmap?.let {
                        Canvas(it)
                    }
                }
                arena!!.copyTerrain(terr)
                val cellWidth: Float = terr[0]?.size?.let { width.toFloat() / it } ?: 0f
                val cellHeight = height.toFloat() / terr.size
                for (row in terrain!!.indices) {
                    val cellTop = cellHeight * row
                    val cellBottom = cellTop + cellHeight
                    for (col in 0 until (terr[row]?.size ?: 0)) {
                        val cellLeft = cellWidth * col
                        paint?.color = breedColors[terrain!![row]!![col].toInt()]
                        canvas?.drawOval(cellLeft, cellTop, cellLeft + cellWidth, cellBottom, paint!!)
                    }
                }
            }
        }
    }

    companion object {
        private const val MAX_HUE = 360f
        private const val SATURATION = 1f
        private const val BRIGHTNESS = 0.85f
    }

    init {
        setWillNotDraw(false)
        paint = Paint()
        paint?.style = Paint.Style.FILL
    }
}