package moe.shizuku.manager

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp


@Composable
fun IdText(id:Int) {
    Text(stringResource(id = id))
}

@Composable
fun cardButton(
    textId:Int,
    iconId:Int,
    onClick: () -> Unit,
    modifier:Modifier = Modifier
        .padding(horizontal = 15.dp)
        .padding(bottom = 15.dp)) {

    Button(modifier = modifier,
        onClick = onClick
    ) { Row() {
        Image(
            painter = painterResource(id = iconId), // Load the drawable
            contentDescription = "About icon", // Provide a description
        )
        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
        Text(text = stringResource(id = textId), modifier = Modifier.align(Alignment.CenterVertically)) }
    }
}

@Composable
fun cardText(text: String) {
    Text(modifier = Modifier
        .padding(bottom = 15.dp)
        .padding(horizontal = 15.dp)
        .fillMaxWidth(),
        text = text)
}


//Universal card template for every section in the mainPage
@Composable
fun DetailedCard(title:String, cornerIcon:Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .wrapContentSize()
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 15.dp)
            .clip(RoundedCornerShape(30.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer, //Card background color
            contentColor = Color.Black  //Card content color,e.g.text
        )
    ) {
        Column() {
            Row() {
                //ICON
                Box(
                    modifier = Modifier
                        .padding(15.dp)
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Image(
                        painter = painterResource(id = cornerIcon), // Load the drawable
                        contentDescription = "About icon", // Provide a description
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize())
                }
                //SIDE TEXT
                Text(modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(), text = title)
            }
            //INSERT CONTENT FROM FUNCTION INPUT
            content()
        }
    }
}




//Modified text element that has hyperlink support
@Composable
fun HyperlinkText(
    htmlText: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    colorLink: Color = Color.Blue,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontSynthesis: FontSynthesis? = null,
    fontFamily: FontFamily? = null,
    fontFeatureSettings: String? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    baselineShift: BaselineShift? = null,
    textGeometricTransform: TextGeometricTransform? = null,
    localeList: LocaleList? = null,
    background: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
    textDecorationLink: TextDecoration? = TextDecoration.Underline,
    shadow: Shadow? = null,
    platformStyle: PlatformSpanStyle? = null,
    drawStyle: DrawStyle? = null


) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        // Parse the HTML text and add annotations for the hyperlinks
        val pattern = "<b><a href=\"(.*?)\">(.*?)</a></b>".toRegex()
        val matcher = pattern.findAll(htmlText)

        var lastIndex = 0
        matcher.forEach { matchResult ->
            val (url, text) = matchResult.destructured
            val range = matchResult.range

            // Append the text before the hyperlink
            if (range.first > lastIndex) {
                withStyle(style = SpanStyle(
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    fontSynthesis = fontSynthesis,
                    fontFamily = fontFamily,
                    fontFeatureSettings = fontFeatureSettings,
                    letterSpacing = letterSpacing,
                    baselineShift = baselineShift,
                    textGeometricTransform = textGeometricTransform,
                    localeList = localeList,
                    background = background,
                    textDecoration = textDecoration,
                    shadow = shadow,
                    platformStyle = platformStyle,
                    drawStyle = drawStyle,)
                ) { append(htmlText.substring(lastIndex, range.first)) }

            }

            // Append the hyperlink text and add an annotation
            withStyle(style = SpanStyle(
                color = colorLink,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = textDecorationLink,
                shadow = shadow,
                platformStyle = platformStyle,
                drawStyle = drawStyle,)
            ) { append(text) }
            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = length - text.length,
                end = length
            )
            lastIndex = range.last + 1
        }
        // Append the remaining text
        if (htmlText.length > lastIndex) {
            withStyle(style = SpanStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = textDecoration,
                shadow = shadow,
                platformStyle = platformStyle,
                drawStyle = drawStyle,
            )
            ) { append(htmlText.substring(lastIndex)) }

        }
    }

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    // Open the URL in the browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                    context.startActivity(intent)
                }
        }
    )
}


@Composable
@Preview
fun Mainpagebackup() {
    val scrollstate = rememberScrollState()
    Column(modifier = Modifier.run { fillMaxSize().verticalScroll(state = scrollstate, enabled = true) }) {
        //SHIZUKU STATUS (IS RUNNING, IS NOT RUNNING)
        DetailedCard(
            stringResource(
                id = R.string.home_status_service_not_running,
                stringResource(id = R.string.app_name)
            ),
            R.drawable.ic_action_about_24dp,
            {} // the {} is defined for content under the main title and icon
        )

        //START VIA WIRELESS DEBUGGING
        DetailedCard(
            stringResource(id = R.string.home_wireless_adb_title),
            R.drawable.ic_action_about_24dp)
        {
            cardText(
                stringResource(id = R.string.home_wireless_adb_description)
                    .replace("""<p>""", "\n\n")
            )

            cardButton(textId = R.string.home_wireless_adb_view_guide_button,
                modifier = Modifier.padding(horizontal = 15.dp),
                iconId = R.drawable.ic_action_about_24dp,
                onClick = {})

            cardButton(textId = R.string.adb_pairing,
                modifier = Modifier.padding(horizontal = 15.dp),
                iconId = R.drawable.ic_action_about_24dp,
                onClick = {})

            cardButton(textId = R.string.home_root_button_start,
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 15.dp),
                iconId = R.drawable.ic_action_about_24dp,
                onClick = {})



        }

        //START BY CONNECTING TO A COMPUTER
        DetailedCard(stringResource(id = R.string.home_adb_title), R.drawable.ic_action_about_24dp)
        {
            HyperlinkText(stringResource(id = R.string.home_adb_description, Helps.ADB.get()),
                Modifier
                    .padding(bottom = 15.dp)
                    .padding(horizontal = 15.dp)
            )
            cardButton(textId = R.string.home_adb_button_view_command,
                iconId = R.drawable.ic_action_about_24dp,
                onClick = {})
        }

        //START FOR ROOTED DEVICES

        DetailedCard(stringResource(id = R.string.home_root_title), R.drawable.ic_action_about_24dp)
        {
            HyperlinkText(stringResource(id = R.string.home_root_description,
                "<b><a href=\"https://dontkillmyapp.com/\">Don\'t kill my app!</a></b>"),
                Modifier
                    .padding(bottom = 15.dp)
                    .padding(horizontal = 15.dp)
            )
            HyperlinkText(stringResource(id = R.string.home_root_description_sui,
                "<b><a href=\"${Helps.SUI.get()}\">Sui</a></b>",
                "Sui"),
                Modifier
                    .padding(bottom = 15.dp)
                    .padding(horizontal = 15.dp)
            )
            cardButton(textId = R.string.home_adb_button_view_command,
                iconId = R.drawable.ic_action_about_24dp,
                onClick = {})

        }


    }
}