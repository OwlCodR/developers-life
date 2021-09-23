package com.developerlife.com

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.developerlife.com.ui.theme.DeveloperLifeTheme
import com.google.accompanist.pager.*
import com.google.gson.Gson
import com.skydoves.landscapist.glide.GlideImage
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import com.google.gson.reflect.TypeToken

fun log(msg: String) {
    Log.d("MainActivity", msg)
}

/**
 * https://developerslife.ru/<id>?json=true - GET post by id
 * https://developerslife.ru/<section>/<page>?json=true - GET posts by section and page
 * https://developerslife.ru/random?json=true - GET random post
 */

@ExperimentalPagerApi
@ExperimentalAnimationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun MainScreen(tabsModel: TabsModel = viewModel()) {
    val tabsTitles = listOf(TabItem.Latest.title, TabItem.Top.title, TabItem.Hot.title)
    val tabsContents by tabsModel.tabs.observeAsState(remember { mutableStateListOf(TabItem.Latest, TabItem.Top, TabItem.Hot) })
    var currentPage by remember { mutableStateOf(0)}

    DeveloperLifeTheme {
        Column {
            TopBar()

            Tabs(
                tabsTitles = tabsTitles,
                page = currentPage,
                onTabSelected = {
                    log("currentPage = $currentPage")
                    currentPage = it
                }
            )

            TabContent(
                currentTabContent = tabsContents[currentPage],
                updateTabContent = { newTab ->
                    tabsContents[currentPage] = newTab
                }
            )

            log("currentPage = $currentPage")
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun TabContent (
    currentTabContent: TabItem,
    updateTabContent: (newTab: TabItem) -> Unit) {

    var currentPost = Post(0, "", "")
    val currentPostIndex = currentTabContent.currentPostIndex

    log("TabsContent() tab.lastShownPostIndex = $currentPostIndex")

    if (currentPostIndex < currentTabContent.posts.size) {
        currentPost = currentTabContent.posts.elementAt(currentPostIndex)
    } else {
        loadPost(currentTabContent) {
            currentTabContent.posts.add(it)
            updateTabContent(currentTabContent)
        }
    }

    Post(currentPost)

    NavigationButtons(onPrevPost = {
        if (currentPostIndex > 0) {
                currentTabContent.currentPostIndex--
                updateTabContent(currentTabContent)
            }
        },
        onNextPost = {
            currentTabContent.currentPostIndex++
            updateTabContent(currentTabContent)
        }
    )
}

fun loadPost(tab: TabItem, updatePost: (Post) -> Unit) {
    log("\nloadPost ${tab.title} " +
            "tab.lastShownPostIndex -> ${tab.currentPostIndex} " +
            "tab.posts.size -> ${tab.posts.size} ")

    if (tab.currentPostIndex > tab.posts.size - 1) {

        var url = "https://developerslife.ru/"

        val pageNumber: Int = tab.posts.size / 5
        url += "${tab.section}/${pageNumber + 1}?json=true"

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
                    val listType = object : TypeToken<List<Post>>() {}.type
                    val postsJson = gson.fromJson<List<Post>>(json, listType)

                    log("url = $url")
                    log("text = $text")
                    log("json = $json")
                    log("PostsJson = $postsJson")

                    if (postsJson.isNotEmpty())
                        updatePost(postsJson[(tab.currentPostIndex + 1) % 5])
                    else
                        updatePost(Post(0, "", ""))
                }
            }
        })
    }
}

@ExperimentalAnimationApi
@Composable
fun Post(post: Post) {
    log("Making New Post '${post.id}'")

    val cardHeight = 500.dp

    Card (
        shape = RoundedCornerShape(30.dp),
        elevation = 10.dp,
        modifier = Modifier.padding(30.dp, 30.dp, 30.dp, 0.dp).height(cardHeight)
    ) {
        Box {
            GlideImage(
                modifier = Modifier.fillMaxSize(),
                imageModel = post.gifURL,
                requestOptions = RequestOptions()
                    .override(1024, 1024)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                failure = {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Image request failed")
                    }
                }
            )

            if (post.title.isNotEmpty()) {
                Row (
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black),
                                cardHeight.value * 2F,
                                Float.POSITIVE_INFINITY
                            )
                        ),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Title(post.title)
                }
            } else {
                Row (verticalAlignment = Alignment.Bottom) {
                    Title(post.title)
                }
            }
        }
    }
}

@Composable
fun Title(title: String) {
    Text(
        modifier = Modifier.padding(20.dp),
        color = Color.White,
        fontSize = 18.sp,
        text = title
    )
}

@Composable
fun NavigationButtons(onPrevPost: () -> Unit, onNextPost: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onPrevPost() }) {
            Text("Назад")
        }

        Spacer(Modifier.padding(10.dp))

        Button(onClick = { onNextPost() }) {
            Text("Вперед")
        }
    }
}

@ExperimentalPagerApi
@Composable
fun Tabs(page: Int, tabsTitles: List<String>, onTabSelected: (Int) -> Unit) {
    log("Tabs updated!")
    TabRow(
        selectedTabIndex = page,
        backgroundColor = Color.White,
        contentColor = Color.Black
    ) {
        tabsTitles.forEachIndexed { index, title ->
            Tab(
                text = { Text(title) },
                selected = page == index,
                onClick = { onTabSelected(index) },
            )
        }
    }
}

@Composable
fun TopBar () {
    TopAppBar(
        title = { Text(text = "Developer Life") },
        backgroundColor = Color.White,
        contentColor = Color.Black,
        elevation = 2.dp
    )
}