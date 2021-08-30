package com.example.shnake

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shnake.R.drawable.*
import com.example.shnake.SnakeCore.gameSpeed
import com.example.shnake.SnakeCore.isPlay
import com.example.shnake.SnakeCore.startTheGame
import kotlinx.android.synthetic.main.activity_main.*

const val HEAD_SIZE = 100


class MainActivity : AppCompatActivity() {

    var pref: SharedPreferences? = null
    var co = 0

    private val allTale = mutableListOf<PartOfTale>() //список частей хвоста

    private val human by lazy {
        ImageView(this).apply {
            this.layoutParams = FrameLayout.LayoutParams(HEAD_SIZE, HEAD_SIZE)
            this.setImageResource(ic_person)
        }
    }
    private val head by lazy {
        ImageView(this).apply {
            this.layoutParams = FrameLayout.LayoutParams(HEAD_SIZE, HEAD_SIZE) // размер головы
            this.setImageResource(circle)/* присвоение созданного объекта (голова) */
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences("TABLE", Context.MODE_PRIVATE)
        co = pref?.getInt("counter", 0)!!
        textVie.text = "Счёт : ${allTale.size}"
        textRecord.text = "Рекорд: $co"

        startTheGame()
        generateNewHuman()
        SnakeCore.nextMove = { move(Directions.BOTTOM, head) }

        // что делаем при клике
        ivArrowUp.setOnClickListener {
            SnakeCore.nextMove = { move(Directions.UP, head) }
        } // передача направления вверх
        ivArrowRight.setOnClickListener {
            SnakeCore.nextMove = { move(Directions.RIGHT, head) }
        } // передача направления вправо
        ivArrowBottom.setOnClickListener {
            SnakeCore.nextMove = { move(Directions.BOTTOM, head) }
        } // передача направления вниз
        ivArrowLeft.setOnClickListener {
            SnakeCore.nextMove = { move(Directions.LEFT, head) }
        } // передача направления влево

        pause.setOnClickListener { // смена кнопки
            if (isPlay) pause.setImageResource(ic_baseline_play)
            else pause.setImageResource(ic_baseline_pause)
            isPlay = !isPlay
        }
    }

    // хранение примитивных данных
    fun saveData(res: Int) {
        val editor = pref?.edit()
        editor?.putInt("counter", res)
        editor?.apply()
    }

    // генерируем элементы
    private fun generateNewHuman() {
        val viewCoordinate = generateHumanCoordinats()

        (human.layoutParams as FrameLayout.LayoutParams).topMargin = viewCoordinate.top
        (human.layoutParams as FrameLayout.LayoutParams).leftMargin = viewCoordinate.left
        container.removeView(human) // удаление еды
        container.addView(human)  // добавление еды
    }

    // проверка чтобы еда не попадала в змею
    private fun generateHumanCoordinats():ViewCoordinate{
        val viewCoordinate = ViewCoordinate( // возвращает координаты еды
            (0..10).random() * HEAD_SIZE,
            (0..10).random() * HEAD_SIZE
        )
        for (partTale in allTale){
            if (partTale.viewCoordinate == viewCoordinate){
                return generateHumanCoordinats()
            }
        }
        if (head.top == viewCoordinate.top && head.left == viewCoordinate.left){
            return generateHumanCoordinats()
        }
        return viewCoordinate
    }

    // проверка съела ли змея еду
    private fun checkIfSnakeEatsPerson(head: View) {
        if (head.left == human.left && head.top == human.top) {
            generateNewHuman()
            addPartOfTale(head.top, head.left)
            textVie.text = "Счёт : ${allTale.size}"
            increaseDifficult()

            if (allTale.size > co){
                saveData(allTale.size)
                co = pref?.getInt("counter", 0)!!
                textRecord.text = "Рекорд: $co"
            }
        }
    }

    // усложняем игру
    private fun increaseDifficult(){
        if (gameSpeed <= MINIMUM_GAME_SPEED){
            return
        }
        if (allTale.size % 5 == 0){
            gameSpeed -= 50
            val toast = Toast.makeText(applicationContext,"Скорость увеличена!",Toast.LENGTH_SHORT)
            with(toast, {
                setGravity(Gravity.CENTER,0,0)
                show()
            })
        }
    }

    // добавление частей хвоста в массив
    private fun addPartOfTale(top: Int, left: Int) {
        val talePart = drawPartOfTale(top, left)
        allTale.add(PartOfTale(ViewCoordinate(top, left), talePart))
    }

    // отрисовка хвоста
    private fun drawPartOfTale(top: Int, left: Int): ImageView {
        val taleImage = ImageView(this)
        taleImage.setImageResource(body)
        taleImage.layoutParams = FrameLayout.LayoutParams(HEAD_SIZE, HEAD_SIZE)
        (taleImage.layoutParams as FrameLayout.LayoutParams).topMargin = top
        (taleImage.layoutParams as FrameLayout.LayoutParams).leftMargin = left

        container.addView(taleImage)
        return taleImage
    }

    //  имитируем движение
    fun move(
        directions: Directions,
        head: View
    ) {
        when (directions) {  // ищем какая кнопка была нажата и поворачиваем
            Directions.UP -> (head.layoutParams as FrameLayout.LayoutParams).topMargin -= HEAD_SIZE
            Directions.RIGHT -> (head.layoutParams as FrameLayout.LayoutParams).leftMargin += HEAD_SIZE
            Directions.BOTTOM -> (head.layoutParams as FrameLayout.LayoutParams).topMargin += HEAD_SIZE
            Directions.LEFT -> (head.layoutParams as FrameLayout.LayoutParams).leftMargin -= HEAD_SIZE

        }
        runOnUiThread { // имитация движения
            if (SheckIfSnakeSmash(head)) {
                isPlay = false
                showScore()
                return@runOnUiThread
            }
            makeTaleMove(head.top, head.left) // движение тела змейки
            checkIfSnakeEatsPerson(head)
            container.removeView(head) // удаляет объект на контейнере
            container.addView(head)// добавляем объект на контейнер лайоут
        }
    }

    private fun showScore() {
        AlertDialog.Builder(this)
            .setTitle("Ваш счёт: ${allTale.size}")
            .setPositiveButton("Начать заново") { _, _ -> this.recreate() }
            .setCancelable(false) // чтобы пользователь не мог убрать окно
            .create()
            .show()
    }

    // проверка врезается ли змейка в хост или выходит за пределы поля
    private fun SheckIfSnakeSmash(head: View): Boolean {
        for (talePart in allTale) {
            if (talePart.viewCoordinate.left == head.left && talePart.viewCoordinate.top == head.top) return true // проверка врезается ли змейка в хост
        }
        if (head.top < 0
            || head.left < 0
            || head.top >= HEAD_SIZE * 16
            || head.left >= HEAD_SIZE * 11
        )  return true// проверка вышла ли змейка за пределы поля

        return false
    }

    // передаём координаты головы змейки
    private fun makeTaleMove(headTop: Int, headLeft: Int) {
        var tempTalePart: PartOfTale? = null
        for (index in 0 until allTale.size) {
            val talePart = allTale[index]
            container.removeView(talePart.imageView) // удалим с контейнера

            if (index == 0) {
                tempTalePart = talePart
                allTale[index] = PartOfTale(ViewCoordinate(headTop, headLeft), drawPartOfTale(headTop, headLeft))
            } else {
                val anotherTempPartOfTale = allTale[index]
                tempTalePart?.let {
                    allTale[index] = PartOfTale(it.viewCoordinate, drawPartOfTale(it.viewCoordinate.top, it.viewCoordinate.left))
                }
                tempTalePart = anotherTempPartOfTale
            }
        }
    }
}
enum class Directions {
    UP,
    RIGHT,
    BOTTOM,
    LEFT
}