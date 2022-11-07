package ink.duo3.xdnmb.android.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.duo3.xdnmb.android.R

@Composable
fun ThreadCard(/*thread: Thread*/) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier.padding(16.dp)
        ) {
            Row(
                Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "MP9RwAc",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                )
                Text(
                    text = "6分钟前",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row() {
                Column(
                    Modifier.weight(1f)
                ) {
                    Column(
                        Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "This is Title",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "This is author",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                        )
                    }

                    Text(
                        text = "Body Text",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.comment_black_24dp),
                        "",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(16.dp)
                    )
                }
            }
            Row(
                Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "No.53213787",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.57f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = "综合版1",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    painter = painterResource(id = R.drawable.comment_black_24dp),
                    "",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterVertically)
                )
                Text(
                    text = "13",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 6.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ThreadCardPreview () {
    ThreadCard()
}