package com.ellecity06.state_layout.widget

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes


/**
 * @author Zavier
 * @time 2020/4/10 15:25
 * @des 全局的多状态布局配置
 */
object StateConfig {


    @LayoutRes
    var errorLayout = View.NO_ID
    @LayoutRes
    var emptyLayout = View.NO_ID
    @LayoutRes
    var loadingLayout = View.NO_ID
    internal var retryIds: List<Int>? = null
    internal var onEmpty: (View.(Any?) -> Unit)? = null
    internal var onError: (View.(Any?) -> Unit)? = null
    internal var onLoading: (View.(Any?) -> Unit)? = null

    fun handleError(v: View, tag: Any?) {
        onError?.invoke(v, tag)
    }

    fun handleLoading(v: View, tag: Any?) {
        onLoading?.invoke(v, tag)
    }

    fun handleEmpty(v: View, tag: Any?) {
        onEmpty?.invoke(v, tag)
    }

    fun onEmpty(block: View.(Any?) -> Unit) {
        onEmpty = block
    }

    fun onLoading(block: View.(Any?) -> Unit) {
        onLoading = block
    }

    fun onError(block: View.(Any?) -> Unit) {
        onError = block
    }

    fun setRetryIds(@IdRes vararg ids: Int) {
        retryIds = ids.toList()
    }
}