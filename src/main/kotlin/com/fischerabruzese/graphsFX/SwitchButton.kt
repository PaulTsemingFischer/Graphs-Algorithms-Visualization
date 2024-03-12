package com.fischerabruzese.graphsFX

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle

class SwitchButton : StackPane() {
    private val back = Rectangle(30.0, 10.0, Color.RED)
    private val button = Button()
    private var buttonStyleOff = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: WHITE;"
    private var buttonStyleOn = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 0.2, 0.0, 0.0, 2); -fx-background-color: #00893d;"
    private var state = false

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
        back.fill = Color.valueOf("#ced5da")
        val r = 2.0
        button.shape = Circle(r)
        setAlignment(button, Pos.CENTER_LEFT)
        button.setMaxSize(15.0, 15.0)
        button.setMinSize(15.0, 15.0)
        button.style = buttonStyleOff
    }

    init {
        init()
        val click = EventHandler<MouseEvent> { _ ->
            if (state) {
                button.style = buttonStyleOff
                back.fill = Color.valueOf("#ced5da")
                setAlignment(button, Pos.CENTER_LEFT)
                state = false
            } else {
                button.style = buttonStyleOn
                back.fill = Color.valueOf("#80C49E")
                setAlignment(button, Pos.CENTER_RIGHT)
                state = true
            }
        }

        button.isFocusTraversable = false
        onMouseClicked = click
        button.onMouseClicked = click
    }
}
