package com.fischerabruzese.graphsFX

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import java.util.LinkedList

class SwitchButton : StackPane() {
    private val color1 = Color.PINK
    private val color2 = Color.SKYBLUE
    private val back = Rectangle(30.0, 10.0, color1)
    private val button = Button()
    private var buttonStyleLeft = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: WHITE;"
    private var buttonStyleRight = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: WHITE;"
    val switchedEvents: LinkedList<(SwitchButtonState) -> Unit> = LinkedList()

    enum class SwitchButtonState {
        LEFT, RIGHT
    }

    var state: SwitchButtonState = SwitchButtonState.LEFT
        private set


    private fun init() {
        children.addAll(back, button)
        minWidth = 30.0
        minHeight = 15.0
        back.maxWidth(30.0)
        back.minWidth(30.0)
        back.maxHeight(10.0)
        back.minHeight(10.0)
        back.arcHeight = back.height
        back.arcWidth = back.height
        val r = 2.0
        button.shape = Circle(r)
        setAlignment(button, Pos.CENTER_LEFT)
        button.setMaxSize(15.0, 15.0)
        button.setMinSize(15.0, 15.0)
        button.style = buttonStyleLeft
    }

    init {
        init()
        val click = EventHandler<MouseEvent> { _ ->
            if (state == SwitchButtonState.RIGHT) {
                button.style = buttonStyleLeft
                back.fill = color1
                setAlignment(button, Pos.CENTER_LEFT)
                state = SwitchButtonState.LEFT
            } else {
                button.style = buttonStyleRight
                back.fill = color2
                setAlignment(button, Pos.CENTER_RIGHT)
                state = SwitchButtonState.RIGHT
            }
            for(s in switchedEvents) s(state)
        }

        button.isFocusTraversable = false
        onMouseClicked = click
        button.onMouseClicked = click
    }
}
