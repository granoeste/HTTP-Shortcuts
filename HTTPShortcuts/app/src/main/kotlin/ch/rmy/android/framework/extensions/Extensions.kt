package ch.rmy.android.framework.extensions

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.util.Predicate
import ch.rmy.android.framework.utils.Destroyable
import ch.rmy.android.framework.utils.Destroyer
import io.reactivex.disposables.Disposable

@ColorInt
fun color(context: Context, @ColorRes colorRes: Int): Int =
    ContextCompat.getColor(context, colorRes)

fun drawable(context: Context, @DrawableRes drawableRes: Int): Drawable? =
    AppCompatResources.getDrawable(context, drawableRes)

fun Activity.dimen(@DimenRes dimenRes: Int) =
    dimen(this, dimenRes)

fun dimen(context: Context, @DimenRes dimenRes: Int) =
    context.resources.getDimensionPixelSize(dimenRes)

inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}

inline fun <T> T.applyIf(predicate: Boolean, block: T.() -> Unit): T =
    if (predicate) apply(block) else this

inline fun <T> T.runIf(predicate: Boolean, block: T.() -> T): T =
    if (predicate) block(this) else this

inline fun <T, U> T.runIfNotNull(item: U?, block: T.(U) -> T): T =
    if (item != null) block(this, item) else this

inline fun <T, U> T.runFor(iterable: Iterable<U>, block: T.(U) -> T): T {
    val iterator = iterable.iterator()
    var item = this
    while (iterator.hasNext()) {
        item = block.invoke(item, iterator.next())
    }
    return item
}

fun Disposable.toDestroyable() = object : Destroyable {
    override fun destroy() {
        dispose()
    }
}

fun Disposable.attachTo(destroyer: Destroyer) {
    destroyer.own { dispose() }
}

fun <T> Map<String, T>.getCaseInsensitive(key: String): T? =
    entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

fun <T> MutableCollection<T>.safeRemoveIf(predicate: Predicate<T>) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        removeIf(predicate::test)
    } else {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (predicate.test(item)) {
                iterator.remove()
            }
        }
    }
}

fun <T> T.takeUnlessEmpty(): T? where T : Collection<*> =
    takeUnless { it.isEmpty() }

@CheckResult
fun <T, ID : Any> List<T>.swapped(id1: ID, id2: ID, getId: T.() -> ID?): List<T> {
    val oldPosition = indexOfFirst { it.getId() == id1 }.takeUnless { it == -1 } ?: return this
    val newPosition = indexOfFirst { it.getId() == id2 }.takeUnless { it == -1 } ?: return this
    return toMutableList()
        .also { list ->
            list.add(newPosition, list.removeAt(oldPosition))
        }
}
