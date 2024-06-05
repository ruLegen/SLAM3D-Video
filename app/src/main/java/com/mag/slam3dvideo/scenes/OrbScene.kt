package com.mag.slam3dvideo.scenes

import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.View

/**
 * The OrbScene interface defines the contract for implementing different scenes in an application
 * using the ORB-SLAM system.
 */
interface OrbScene {
    /**
     * Initializes the scene with the provided game engine.
     *
     * @param engine The game engine instance.
     */
    fun init(engine: Engine)
    /**
     * Activates the scene, making it ready for rendering and interaction.
     */
    fun activate()
    /**
     * Called before rendering the scene. Allows performing any necessary setup or modifications
     * to the renderer.
     *
     * @param renderer The renderer instance.
     */
    fun beforeRender(renderer: Renderer)
    fun render(renderer: Renderer)
    fun onResize(width: Int, height: Int)
    fun destroy(engine: Engine)
    /**
     * Updates the scene state. This method can be overridden by implementing classes to include
     * scene-specific logic for updating state.
     */
    fun update(){}
}