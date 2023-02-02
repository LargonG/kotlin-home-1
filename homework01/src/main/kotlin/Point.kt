interface Point: DimensionAware

/**
 * Реализация Point по умолчанию
 *
 * Должны работать вызовы DefaultPoint(10), DefaultPoint(12, 3), DefaultPoint(12, 3, 12, 4, 56)
 * с любым количество параметров
 *
 * Сама коллекция параметров недоступна, доступ - через методы интерфейса
 */
@JvmInline
value class DefaultPoint(private val coords: IntArray): Point {
    override val ndim: Int
        get() = coords.size

    override fun dim(i: Int) = coords[i]

    // JVM clash if only vararg
    constructor(first: Int, vararg other: Int): this(intArrayOf(first, *other))
}