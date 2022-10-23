import java.lang.IllegalArgumentException

interface Point: DimensionAware

/**
 * Реализация Point по умолчанию
 *
 * Должны работать вызовы DefaultPoint(10), DefaultPoint(12, 3), DefaultPoint(12, 3, 12, 4, 56)
 * с любым количество параметров
 *
 * Сама коллекция параметров недоступна, доступ - через методы интерфейса
 */
class DefaultPoint(private vararg val coords: Int): Point {
    override val ndim: Int
        get() = coords.size

    override fun dim(i: Int) = coords[i]
}