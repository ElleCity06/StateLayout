package com.ellecity06.state_layout.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.ellecity06.state_layout.R
import com.ellecity06.state_layout.widget.StateConfig.errorLayout
import com.ellecity06.state_layout.ktx.throttleClick


/**
 * @author Zavier
 * @time 2020/4/10 15:02
 * @des 缺省布局
 */
class StateLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var contentId: Int = -2
    private val contentViews = ArrayMap<Int, View>()
    // 重试按钮的id集合,可以通过config全局配置，也可以通过statelayout去局部配置
    private var retryIds: List<Int>? = null
    // 控制loading的时候是否执行refresh,
    private var refresh = true

    private var onEmpty: (View.(Any?) -> Unit)? = null
    private var onError: (View.(Any?) -> Unit)? = null
    private var onLoading: (View.(Any?) -> Unit)? = null
    private var onRefresh: (StateLayout.(View) -> Unit)? = null

    private var stateChanged = false
    private var trigger = false

    var loaded = false

    var status = Status.CONTENT
        private set


    @LayoutRes
    var errorLayout: Int = NO_ID
        set(value) {
            if (field != value) {
                remove(field)
                field = value
            }
        }

    @LayoutRes
    var emptyLayout: Int = NO_ID
        set(value) {
            if (field != value) {
                remove(field)
                field = value
            }
        }

    @LayoutRes
    var loadingLayout: Int = NO_ID
        set(value) {
            if (field != value) {
                remove(field)
                field = value
            }
        }


    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.StateLayout)
        try {
            emptyLayout = attributes.getResourceId(R.styleable.StateLayout_empty_layout, NO_ID)
            errorLayout = attributes.getResourceId(R.styleable.StateLayout_error_layout, NO_ID)
            loadingLayout = attributes.getResourceId(R.styleable.StateLayout_loading_layout, NO_ID)
        } finally {
            attributes.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 1 || childCount == 0) {
            throw UnsupportedOperationException("StateLayout只能包含一个子视图")
        }

        if (contentViews.size == 0) {
            val view = getChildAt(0)
            setContentView(view)
        }
    }

    /**
     * 当空缺省页显示时
     * @see showEmpty
     * @see StateConfig.onEmpty
     */
    fun onEmpty(block: View.(Any?) -> Unit): StateLayout {
        onEmpty = block
        return this
    }

    /**
     * 当加载中缺省页显示时
     * @see showLoading
     * @see StateConfig.onLoading
     */
    fun onLoading(block: View.(Any?) -> Unit): StateLayout {
        onLoading = block
        return this
    }

    /**
     * 当错误缺省页显示时
     * @see showError
     * @see StateConfig.onError
     */
    fun onError(block: View.(Any?) -> Unit): StateLayout {
        onError = block
        return this
    }

    /**
     * 当[showLoading]时会回调该函数参数, 一般将网络请求等异步操作放入其中
     */
    fun onRefresh(block: StateLayout.(loading: View) -> Unit): StateLayout {
        onRefresh = block
        return this
    }

    /**
     * 有网则显示加载中, 无网络直接显示错误
     */
    fun showLoading(tag: Any? = null, refresh: Boolean = true) {
        this.refresh = refresh

        if (loadingLayout == NO_ID) {
            loadingLayout = StateConfig.loadingLayout
        }

        if (status == Status.LOADING) {
            if (onLoading == null) {
                StateConfig.onLoading?.let { onLoading = it }
            }
            onLoading?.invoke(getView(loadingLayout), tag)
            return
        }

        if (loadingLayout != NO_ID) {
            show(loadingLayout, tag)
        }
    }

    fun showEmpty(tag: Any? = null) {
        if (emptyLayout == NO_ID) {
            emptyLayout = StateConfig.emptyLayout
        }
        if (emptyLayout != NO_ID) {
            show(emptyLayout, tag)
        }
    }

    fun showError(tag: Any? = null) {
        if (errorLayout == NO_ID) {
            errorLayout = StateConfig.errorLayout
        }
        if (errorLayout != NO_ID) {
            show(errorLayout, tag)
        }
    }

    fun showContent() {
        if (trigger && stateChanged) return
        loaded = true
        show(contentId)
    }

    /**
     * 设置[errorLayout]和[emptyLayout]中的视图点击后会执行[StateLayout.showLoading]
     * 并且500ms内防重复点击
     */
    fun setRetryIds(@IdRes vararg ids: Int): StateLayout {
        retryIds = ids.toList()
        return this
    }

    /**
     * 用于网络请求的触发器
     */
    fun trigger(): Boolean {
        trigger = !trigger
        if (!trigger) stateChanged = false
        return trigger
    }


    /**
     * 显示视图
     */
    private fun show(layoutId: Int, tag: Any? = null) {

        if (trigger) {
            stateChanged = true
        }

        runMain {

            for (view in contentViews.values) view.visibility = View.GONE

            try {
                val view = getView(layoutId)
                view.visibility = View.VISIBLE

                when (layoutId) {
                    emptyLayout -> {
                        status = Status.EMPTY
                        if (retryIds == null) retryIds = StateConfig.retryIds

                        retryIds?.forEach {
                            view.findViewById<View>(it)?.throttleClick { showLoading() }
                        }

                        if (onEmpty == null) StateConfig.onEmpty?.let { onEmpty = it }

                        // 执行onEmpty函数
                        onEmpty?.invoke(view, tag)
                    }
                    errorLayout -> {
                        status = Status.ERROR

                        if (retryIds == null) retryIds = StateConfig.retryIds

                        retryIds?.forEach {
                            view.findViewById<View>(it)?.throttleClick { showLoading() }
                        }

                        if (onError == null) StateConfig.onError?.let { onError = it }

                        onError?.invoke(view, tag)
                    }

                    loadingLayout -> {
                        status = Status.LOADING

                        if (onLoading == null) {
                            StateConfig.onLoading?.let { onLoading = it }
                        }

                        onLoading?.invoke(view, tag)

                        if (refresh) onRefresh?.invoke(this, view)
                    }
                    else -> status = Status.CONTENT
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 移除
     */
    private fun remove(layoutId: Int?) {
        if (contentViews.containsKey(layoutId)) {
            val stateView = contentViews.remove(layoutId)
            removeView(stateView)
        }
    }

    @Throws(NullPointerException::class)
    private fun getView(@LayoutRes layoutId: Int): View {
        if (contentViews.containsKey(layoutId)) {
            return contentViews[layoutId]!!
        }
        val view = LayoutInflater.from(context).inflate(layoutId, this, false)
        addView(view)

        contentViews[layoutId] = view
        return view
    }


    /**
     * 标记视图为内容布局, 用于扩展替换内容. 一般情况不需要使用
     */
    fun setContentView(view: View) {
        contentViews[contentId] = view
    }


    /**
     * 确保ui在主线程更新
     */
    private fun runMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post { block() }
        }
    }
}

