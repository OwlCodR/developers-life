package com.developerlife.com

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.developerlife.com.ui.theme.DeveloperLifeTheme
import com.google.accompanist.pager.*
import com.google.gson.Gson
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.NonDisposableHandle.parent


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
    var tabs = listOf(TabItem.Latest, TabItem.Top, TabItem.Hot)
    val pagerState = rememberPagerState(pageCount = tabs.size)

    DeveloperLifeTheme {
        Column {
            TopAppBar(
                title = { Text(text = "Developer Life") },
                backgroundColor = Color.White,
                contentColor = Color.Black,
                elevation = 2.dp
            )
            Tabs(tabs = tabs, pagerState = pagerState )
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
        requestOptions = RequestOptions()
            .override(1024, 1024)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop(),
        loading = {
            Column (
                modifier = Modifier.fillMaxSize()
            ) {
                Box (
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }

            }
        },
        failure = {
            Text(text = "Image request failed")
        }
    )
}

fun loadPosts(tab: TabItem, posts: MutableList<Post>) {
    var url = "https://developerslife.ru/"

    if (tab.lastPostIndex > tab.posts.size - 1) {
        // We need to load 5 more posts

        val page: Int = tab.posts.size / 5
        url += "${tab.section}/${page + 1}?json=true"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val text = response.body!!.string()
                    val json = JSONObject(text).getJSONArray("result").toString()
                    val gson = Gson()
                    val listType = object: TypeToken<List<Post>>(){}.type
                    val postsJson = gson.fromJson<List<Post>>(json, listType)

                    for (post in postsJson) {
                        posts.add(post)
                    }
                }
            }
        })
    }
}

@ExperimentalAnimationApi
@Composable
fun Post(tab: TabItem) {

    val posts by remember { mutableStateOf(tab.posts) }

    loadPosts(tab, posts)

    Card (
        shape = RoundedCornerShape(30.dp),
        elevation = 10.dp,
        modifier = Modifier
            .padding(all = 30.dp)
            .padding(top = 50.dp)
            .padding(bottom = 50.dp)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column {
            var text = ""
            if (posts.size != 0) {
                loadImage(posts[tab.lastPostIndex].gifURL)
                text = posts[tab.lastPostIndex].title
            }

            Text(
                color = Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(all = 20.dp),
                text = text,
                style = MaterialTheme.typography.body1
            )
            Row (
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier.padding(all = 10.dp),
                    onClick = { if (tab.lastPostIndex > 0) tab.lastPostIndex--},
                ) {
                    Text("Назад")
                }
                Button(modifier = Modifier.padding(all = 10.dp),
                    onClick = { if (tab.lastPostIndex + 1 <= posts.size)  tab.lastPostIndex++ },
                ) {
                    Text("Вперед")
                }
            }
        }
    }
}

