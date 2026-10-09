package com.randolph.keeplocal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randolph.keeplocal.util.ImageDecoder

@Composable
fun KeepImageGrid(
    imageUris: List<String>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 220.dp,
    onImageClick: ((Int) -> Unit)? = null,
    onImageRemove: ((Int) -> Unit)? = null
) {
    if (imageUris.isEmpty()) return

    val totalCount = imageUris.size
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
            .clip(shape)
    ) {
        when {
            totalCount == 1 -> {
                GridTileImage(
                    uriString = imageUris[0],
                    reqWidth = 800,
                    reqHeight = 600,
                    contentScale = ContentScale.Crop,
                    onTileClick = { onImageClick?.invoke(0) },
                    onRemove = if (onImageRemove != null) { { onImageRemove(0) } } else null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            totalCount == 2 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridTileImage(
                        uriString = imageUris[0],
                        reqWidth = 400,
                        reqHeight = 600,
                        onTileClick = { onImageClick?.invoke(0) },
                        onRemove = if (onImageRemove != null) { { onImageRemove(0) } } else null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    GridTileImage(
                        uriString = imageUris[1],
                        reqWidth = 400,
                        reqHeight = 600,
                        onTileClick = { onImageClick?.invoke(1) },
                        onRemove = if (onImageRemove != null) { { onImageRemove(1) } } else null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            totalCount == 3 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridTileImage(
                        uriString = imageUris[0],
                        reqWidth = 400,
                        reqHeight = 600,
                        onTileClick = { onImageClick?.invoke(0) },
                        onRemove = if (onImageRemove != null) { { onImageRemove(0) } } else null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridTileImage(
                            uriString = imageUris[1],
                            reqWidth = 400,
                            reqHeight = 300,
                            onTileClick = { onImageClick?.invoke(1) },
                            onRemove = if (onImageRemove != null) { { onImageRemove(1) } } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        GridTileImage(
                            uriString = imageUris[2],
                            reqWidth = 400,
                            reqHeight = 300,
                            onTileClick = { onImageClick?.invoke(2) },
                            onRemove = if (onImageRemove != null) { { onImageRemove(2) } } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
            else -> { // 4 or more
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridTileImage(
                            uriString = imageUris[0],
                            reqWidth = 400,
                            reqHeight = 300,
                            onTileClick = { onImageClick?.invoke(0) },
                            onRemove = if (onImageRemove != null) { { onImageRemove(0) } } else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        GridTileImage(
                            uriString = imageUris[1],
                            reqWidth = 400,
                            reqHeight = 300,
                            onTileClick = { onImageClick?.invoke(1) },
                            onRemove = if (onImageRemove != null) { { onImageRemove(1) } } else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridTileImage(
                            uriString = imageUris[2],
                            reqWidth = 400,
                            reqHeight = 300,
                            onTileClick = { onImageClick?.invoke(2) },
                            onRemove = if (onImageRemove != null) { { onImageRemove(2) } } else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            GridTileImage(
                                uriString = imageUris[3],
                                reqWidth = 400,
                                reqHeight = 300,
                                onTileClick = { onImageClick?.invoke(3) },
                                onRemove = if (onImageRemove != null) { { onImageRemove(3) } } else null,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (totalCount > 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onImageClick?.invoke(3) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${totalCount - 3}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridTileImage(
    uriString: String,
    reqWidth: Int,
    reqHeight: Int,
    onTileClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmapState = remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uriString) {
        bitmapState.value = ImageDecoder.loadDownsampledBitmap(context, uriString, reqWidth, reqHeight)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onTileClick() }
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Note Image Attachment",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Loading image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove Image",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
