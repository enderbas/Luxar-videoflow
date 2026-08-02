package com.luxar.videoflow

import kotlin.math.roundToInt

internal data class ResolvedPlacement(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal object PlacementResolver {
    fun resolve(
        placement: VideoPlacement,
        coordinateSpace: CoordinateSpace,
        containerWidth: Int,
        containerHeight: Int,
    ): ResolvedPlacement {
        if (containerWidth <= 0 || containerHeight <= 0) {
            return ResolvedPlacement(left = 0, top = 0, width = 0, height = 0)
        }

        val scaleX: Float
        val scaleY: Float
        when (coordinateSpace) {
            CoordinateSpace.Normalized -> {
                scaleX = containerWidth.toFloat()
                scaleY = containerHeight.toFloat()
            }
            is CoordinateSpace.Reference -> {
                scaleX = containerWidth.toFloat() / coordinateSpace.width
                scaleY = containerHeight.toFloat() / coordinateSpace.height
            }
        }

        val left = (placement.x * scaleX).roundToInt()
        val top = (placement.y * scaleY).roundToInt()
        return ResolvedPlacement(
            left = left,
            top = top,
            width = (placement.width * scaleX).roundToInt(),
            height = (placement.height * scaleY).roundToInt(),
        )
    }
}
