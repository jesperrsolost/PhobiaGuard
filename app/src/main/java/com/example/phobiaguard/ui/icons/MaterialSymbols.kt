package com.example.phobiaguard.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val CameraIcon: ImageVector
  get() {
    if (_CameraIcon != null) {
      return _CameraIcon!!
    }
    _CameraIcon =
      ImageVector.Builder(
        name = "photo_camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 17.5f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(16.5f, 13f)
            reflectiveQuadTo(15.19f, 9.81f)
            reflectiveQuadTo(12f, 8.5f)
            reflectiveQuadTo(8.81f, 9.81f)
            reflectiveQuadTo(7.5f, 13f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 17.5f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(-1.05f, 0f, -1.77f, -0.72f)
            reflectiveQuadTo(9.5f, 13f)
            reflectiveQuadToRelative(0.73f, -1.78f)
            reflectiveQuadTo(12f, 10.5f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(14.5f, 13f)
            reflectiveQuadToRelative(-0.72f, 1.78f)
            reflectiveQuadTo(12f, 15.5f)
            close()
            moveTo(4f, 21f)
            quadTo(3.18f, 21f, 2.59f, 20.41f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(7f)
            quadTo(2f, 6.18f, 2.59f, 5.59f)
            reflectiveQuadTo(4f, 5f)
            horizontalLineTo(7.15f)
            lineTo(9f, 3f)
            horizontalLineToRelative(6f)
            lineToRelative(1.85f, 2f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 6.18f, 22f, 7f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 21f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 19f)
            horizontalLineTo(20f)
            verticalLineTo(7f)
            horizontalLineTo(15.95f)
            lineTo(14.13f, 5f)
            horizontalLineTo(9.88f)
            lineTo(8.05f, 7f)
            horizontalLineTo(4f)
            verticalLineTo(19f)
            close()
            moveToRelative(8f, -6f)
            close()
          }
        }
        .build()
    return _CameraIcon!!
  }

private var _CameraIcon: ImageVector? = null


@Suppress("CheckReturnValue")
public val EyeIcon: ImageVector
  get() {
    if (_EyeIcon != null) {
      return _EyeIcon!!
    }
    _EyeIcon =
      ImageVector.Builder(
        name = "visibility",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(15.19f, 14.69f)
            quadTo(16.5f, 13.38f, 16.5f, 11.5f)
            reflectiveQuadTo(15.19f, 8.31f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadTo(8.81f, 8.31f)
            reflectiveQuadTo(7.5f, 11.5f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(3.19f, -1.31f)
            close()
            moveToRelative(-5.1f, -1.28f)
            quadTo(9.3f, 12.63f, 9.3f, 11.5f)
            reflectiveQuadTo(10.09f, 9.59f)
            reflectiveQuadTo(12f, 8.8f)
            reflectiveQuadToRelative(1.91f, 0.79f)
            quadToRelative(0.79f, 0.79f, 0.79f, 1.91f)
            reflectiveQuadToRelative(-0.79f, 1.91f)
            reflectiveQuadTo(12f, 14.2f)
            reflectiveQuadTo(10.09f, 13.41f)
            close()
            moveTo(5.35f, 16.96f)
            quadTo(2.35f, 14.93f, 1f, 11.5f)
            quadTo(2.35f, 8.07f, 5.35f, 6.04f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(6.65f, 2.04f)
            reflectiveQuadTo(23f, 11.5f)
            quadToRelative(-1.35f, 3.42f, -4.35f, 5.46f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(5.35f, 16.96f)
            close()
            moveTo(12f, 11.5f)
            close()
            moveToRelative(5.19f, 4.01f)
            quadTo(19.55f, 14.02f, 20.8f, 11.5f)
            quadTo(19.55f, 8.98f, 17.19f, 7.49f)
            reflectiveQuadTo(12f, 6f)
            quadTo(9.18f, 6f, 6.81f, 7.49f)
            reflectiveQuadTo(3.2f, 11.5f)
            quadToRelative(1.25f, 2.52f, 3.61f, 4.01f)
            reflectiveQuadTo(12f, 17f)
            reflectiveQuadToRelative(5.19f, -1.49f)
            close()
          }
        }
        .build()
    return _EyeIcon!!
  }

private var _EyeIcon: ImageVector? = null

@Suppress("CheckReturnValue")
val PersonIcon: ImageVector
  get() {
    if (_PersonIcon != null) {
      return _PersonIcon!!
    }
    _PersonIcon =
      ImageVector.Builder(
          name = "person",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.18f, 10.83f)
            quadTo(8f, 9.65f, 8f, 8f)
            reflectiveQuadTo(9.18f, 5.18f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(2.83f, 1.18f)
            reflectiveQuadTo(16f, 8f)
            reflectiveQuadToRelative(-1.17f, 2.82f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadTo(9.18f, 10.83f)
            close()
            moveTo(4f, 20f)
            verticalLineTo(17.2f)
            quadTo(4f, 16.35f, 4.44f, 15.64f)
            quadTo(4.88f, 14.93f, 5.6f, 14.55f)
            quadTo(7.15f, 13.77f, 8.75f, 13.39f)
            reflectiveQuadTo(12f, 13f)
            reflectiveQuadToRelative(3.25f, 0.39f)
            reflectiveQuadToRelative(3.15f, 1.16f)
            quadToRelative(0.72f, 0.38f, 1.16f, 1.09f)
            reflectiveQuadTo(20f, 17.2f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(6f, 18f)
            horizontalLineTo(18f)
            verticalLineTo(17.2f)
            quadToRelative(0f, -0.27f, -0.14f, -0.5f)
            quadTo(17.73f, 16.48f, 17.5f, 16.35f)
            quadTo(16.15f, 15.68f, 14.78f, 15.34f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadTo(9.23f, 15.34f)
            reflectiveQuadTo(6.5f, 16.35f)
            quadTo(6.28f, 16.48f, 6.14f, 16.7f)
            quadTo(6f, 16.93f, 6f, 17.2f)
            verticalLineTo(18f)
            close()
            moveTo(13.41f, 9.41f)
            quadTo(14f, 8.82f, 14f, 8f)
            reflectiveQuadTo(13.41f, 6.59f)
            reflectiveQuadTo(12f, 6f)
            reflectiveQuadTo(10.59f, 6.59f)
            quadTo(10f, 7.18f, 10f, 8f)
            reflectiveQuadToRelative(0.59f, 1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadTo(13.41f, 9.41f)
            close()
            moveTo(12f, 8f)
            close()
            moveToRelative(0f, 10f)
            close()
          }
        }
        .build()
    return _PersonIcon!!
  }

private var _PersonIcon: ImageVector? = null

private fun menuSymbol(
  name: String,
  content: ImageVector.Builder.() -> Unit
): ImageVector = ImageVector.Builder(
  name = name,
  defaultWidth = 24.dp,
  defaultHeight = 24.dp,
  viewportWidth = 24f,
  viewportHeight = 24f
).apply(content).build()

val DeveloperIcon: ImageVector by lazy {
  menuSymbol("code") {
    path(fill = SolidColor(Color.Black)) {
      moveTo(8.6f, 6.6f)
      lineTo(7.2f, 5.2f)
      lineTo(1.4f, 12f)
      lineTo(7.2f, 18.8f)
      lineTo(8.6f, 17.4f)
      lineTo(4f, 12f)
      close()
      moveTo(15.4f, 6.6f)
      lineTo(16.8f, 5.2f)
      lineTo(22.6f, 12f)
      lineTo(16.8f, 18.8f)
      lineTo(15.4f, 17.4f)
      lineTo(20f, 12f)
      close()
      moveTo(14.3f, 4f)
      lineTo(10.8f, 20f)
      lineTo(9.1f, 20f)
      lineTo(12.6f, 4f)
      close()
    }
  }
}

val LockIcon: ImageVector by lazy {
  menuSymbol("lock") {
    path(
      fill = SolidColor(Color.Black),
      pathFillType = PathFillType.EvenOdd
    ) {
      moveTo(6f, 10f)
      horizontalLineTo(18f)
      quadTo(19f, 10f, 19f, 11f)
      verticalLineTo(20f)
      quadTo(19f, 21f, 18f, 21f)
      horizontalLineTo(6f)
      quadTo(5f, 21f, 5f, 20f)
      verticalLineTo(11f)
      quadTo(5f, 10f, 6f, 10f)
      close()
      moveTo(12f, 13f)
      curveTo(10.9f, 13f, 10f, 13.9f, 10f, 15f)
      curveTo(10f, 16.1f, 10.9f, 17f, 12f, 17f)
      curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f)
      curveTo(14f, 13.9f, 13.1f, 13f, 12f, 13f)
      close()
    }
    path(
      fill = null,
      stroke = SolidColor(Color.Black),
      strokeLineWidth = 2.2f,
      strokeLineCap = StrokeCap.Round
    ) {
      moveTo(8f, 10f)
      verticalLineTo(7f)
      curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
      curveTo(14.2f, 3f, 16f, 4.8f, 16f, 7f)
      verticalLineTo(10f)
    }
  }
}

val SecurityIcon: ImageVector by lazy {
  menuSymbol("shield") {
    path(
      fill = SolidColor(Color.Black),
      pathFillType = PathFillType.EvenOdd
    ) {
      moveTo(12f, 2f)
      lineTo(20f, 5.2f)
      verticalLineTo(11.1f)
      curveTo(20f, 16.1f, 16.6f, 20.5f, 12f, 22f)
      curveTo(7.4f, 20.5f, 4f, 16.1f, 4f, 11.1f)
      verticalLineTo(5.2f)
      close()
      moveTo(12f, 4.2f)
      lineTo(6f, 6.6f)
      verticalLineTo(11.1f)
      curveTo(6f, 15.1f, 8.5f, 18.7f, 12f, 20f)
      curveTo(15.5f, 18.7f, 18f, 15.1f, 18f, 11.1f)
      verticalLineTo(6.6f)
      close()
    }
  }
}

val SettingsIcon: ImageVector by lazy {
  menuSymbol("settings") {
    path(
      fill = SolidColor(Color.Black),
      pathFillType = PathFillType.EvenOdd
    ) {
      moveTo(10f, 2f)
      horizontalLineTo(14f)
      lineTo(14.5f, 4.3f)
      lineTo(16.3f, 5f)
      lineTo(18.3f, 3.7f)
      lineTo(21f, 6.4f)
      lineTo(19.7f, 8.4f)
      lineTo(20.4f, 10.2f)
      lineTo(22.7f, 10.7f)
      verticalLineTo(13.3f)
      lineTo(20.4f, 13.8f)
      lineTo(19.7f, 15.6f)
      lineTo(21f, 17.6f)
      lineTo(18.3f, 20.3f)
      lineTo(16.3f, 19f)
      lineTo(14.5f, 19.7f)
      lineTo(14f, 22f)
      horizontalLineTo(10f)
      lineTo(9.5f, 19.7f)
      lineTo(7.7f, 19f)
      lineTo(5.7f, 20.3f)
      lineTo(3f, 17.6f)
      lineTo(4.3f, 15.6f)
      lineTo(3.6f, 13.8f)
      lineTo(1.3f, 13.3f)
      verticalLineTo(10.7f)
      lineTo(3.6f, 10.2f)
      lineTo(4.3f, 8.4f)
      lineTo(3f, 6.4f)
      lineTo(5.7f, 3.7f)
      lineTo(7.7f, 5f)
      lineTo(9.5f, 4.3f)
      close()
      moveTo(12f, 8.5f)
      curveTo(10.1f, 8.5f, 8.5f, 10.1f, 8.5f, 12f)
      curveTo(8.5f, 13.9f, 10.1f, 15.5f, 12f, 15.5f)
      curveTo(13.9f, 15.5f, 15.5f, 13.9f, 15.5f, 12f)
      curveTo(15.5f, 10.1f, 13.9f, 8.5f, 12f, 8.5f)
      close()
    }
  }
}

val HelpIcon: ImageVector by lazy {
  menuSymbol("help") {
    path(
      fill = SolidColor(Color.Black),
      pathFillType = PathFillType.EvenOdd
    ) {
      moveTo(12f, 2f)
      curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
      curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
      curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
      curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
      close()
      moveTo(12f, 4f)
      curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
      curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
      curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
      curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
      close()
    }
    path(fill = SolidColor(Color.Black)) {
      moveTo(11f, 16f)
      verticalLineTo(15.2f)
      curveTo(11f, 13.7f, 11.7f, 13f, 12.8f, 12.2f)
      curveTo(13.8f, 11.5f, 14.3f, 11f, 14.3f, 10.2f)
      curveTo(14.3f, 9.1f, 13.4f, 8.3f, 12f, 8.3f)
      curveTo(10.6f, 8.3f, 9.7f, 9.1f, 9.7f, 10.3f)
      horizontalLineTo(7.6f)
      curveTo(7.6f, 7.9f, 9.4f, 6.2f, 12f, 6.2f)
      curveTo(14.6f, 6.2f, 16.4f, 7.9f, 16.4f, 10.2f)
      curveTo(16.4f, 11.9f, 15.4f, 12.8f, 14.1f, 13.7f)
      curveTo(13.4f, 14.2f, 13.1f, 14.6f, 13.1f, 15.2f)
      verticalLineTo(16f)
      close()
      moveTo(10.9f, 17f)
      horizontalLineTo(13.1f)
      verticalLineTo(19f)
      horizontalLineTo(10.9f)
      close()
    }
  }
}

val ChevronRightIcon: ImageVector by lazy {
  menuSymbol("chevron_right") {
    path(fill = SolidColor(Color.Black)) {
      moveTo(9f, 5f)
      lineTo(7.6f, 6.4f)
      lineTo(13.2f, 12f)
      lineTo(7.6f, 17.6f)
      lineTo(9f, 19f)
      lineTo(16f, 12f)
      close()
    }
  }
}
