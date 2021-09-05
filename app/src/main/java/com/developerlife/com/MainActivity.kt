package com.developerlife.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.developerlife.com.ui.theme.DeveloperLifeTheme
import com.google.accompanist.pager.*
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import okhttp3.*
import okio.IOException
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    @ExperimentalPagerApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val tabs = listOf(TabItem.Latest, TabItem.Top, TabItem.Hot)
            val pagerState = rememberPagerState(pageCount = tabs.size)

            DeveloperLifeTheme {
                Column {
                    TopAppBar(
                        title = { Text(text = "Developer Life") },
                        backgroundColor = Color.White,
                        contentColor = Color.Black,
                        elevation = 2.dp
                    )
                    Tabs(tabs = tabs, pagerState = pagerState)
                    TabsContent(tabs = tabs, pagerState = pagerState)
                }
            }
        }
    }
}

/**
 * https://developerslife.ru/<id>?json=true - GET post by id
 * https://developerslife.ru/<section>/<page>?json=true - GET posts by section and page
 * https://developerslife.ru/random?json=true - GET random post
 */

@ExperimentalAnimationApi
@Preview(showSystemUi = true, showBackground = true)
@ExperimentalPagerApi
@Composable
fun DefaultPreview() {
    val tabs = listOf(TabItem.Latest, TabItem.Top, TabItem.Hot)
    val pagerState = rememberPagerState(pageCount = tabs.size)

    DeveloperLifeTheme {
        Column {
            TopAppBar(
                title = { Text(text = "Developer Life") },
                backgroundColor = Color.White,
                contentColor = Color.Black,
                elevation = 2.dp
            )
            Tabs(tabs = tabs, pagerState = pagerState)
            TabsContent(tabs = tabs, pagerState = pagerState)
        }
    }
}

@ExperimentalPagerApi
@Composable
fun Tabs(tabs: List<TabItem>, pagerState: PagerState) {
    val scope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = Color.White,
        contentColor = Color.Black,
        indicator = {
                tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                text = { Text(tab.title) },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun TabsContent(tabs: List<TabItem>, pagerState: PagerState) {
    HorizontalPager(state = pagerState) { page ->
        Post(tabs[page])
    }
}

@Composable
fun loadImage(contentUrl: String) {
    GlideImage(
        imageModel = contentUrl,
        // Crop, Fit, Inside, FillHeight, FillWidth, None
        contentScale = ContentScale.Crop,
        // shows an error ImageBitmap when the request failed.
        error = ImageBitmap.imageResource(R.drawable.error)
    )
}


@ExperimentalAnimationApi
@Composable
fun Post(tab: TabItem) {
    var url = "https://developerslife.ru/"

    var loadingState by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }

    if (tab.lastPostIndex > tab.posts.size - 1) {
        // We need to load 5 more posts

        val page: Int = tab.posts.size / 5
        url += "${tab.section}/${page + 1}?json=true"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        AnimatedVisibility(visible = loadingState) {
            CircularProgressIndicator()
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // loadingState = !loadingState

                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                // loadingState = !loadingState

                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    // JSON = JSONObject(response.body!!.string()).toString()
                    // println(JSON)
                }
            }
        })
    }

    Card (
        shape = RoundedCornerShape(30.dp),
        elevation = 10.dp,
        modifier = Modifier
            .padding(all = 30.dp)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column {
            val contentUrl = ""
            //loadImage(contentUrl)

            Text(
                modifier = Modifier
                    .padding(all = 20.dp)
                    .align(Alignment.CenterHorizontally),
                text = "Content: $JSON",
                style = MaterialTheme.typography.body1
            )
        }
    }
}

