package com.ellecity06.state_layout.widget

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * @author Zavier
 * @time 2020/4/10 15:19
 * @des 针对于StateLayout 的创建的工具类
 */
/**
 * 在activity代码创建StateLayout
 */
fun Activity.state(): StateLayout {
    val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
    return view.state()
}

/**
 * 在fragment里代码创建StateLayout
 */
fun androidx.fragment.app.Fragment.state(): StateLayout {
    val stateLayout = view!!.state()

    lifecycle.addObserver(object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun removeState() {
            val parent = stateLayout.parent as ViewGroup
            parent.removeView(stateLayout)
            lifecycle.removeObserver(this)
        }
    })

    return stateLayout
}

/**
 * 在一个view里创建StateLayout
 */
fun View.state(): StateLayout {
    val stateLayout = StateLayout(context)
    val parent = parent as ViewGroup
    stateLayout.id = id
    val index = parent.indexOfChild(this)
    val layoutParams = layoutParams
    parent.removeView(this)
    parent.addView(stateLayout, index, layoutParams)

    if (this is ConstraintLayout) {
        val contentViewLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        stateLayout.addView(this, contentViewLayoutParams)
    } else {
        stateLayout.addView(this)
    }
    stateLayout.setContentView(this)

    return stateLayout
}