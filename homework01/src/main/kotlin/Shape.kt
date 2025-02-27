
interface Shape: DimensionAware, SizeAware

/**
 * Реализация Shape по умолчанию
 *
 * Должны работать вызовы DefaultShape(10), DefaultShape(12, 3), DefaultShape(12, 3, 12, 4, 56)
 * с любым количество параметров
 *
 * При попытке создать пустой Shape бросается EmptyShapeException
 *
 * При попытке указать неположительное число по любой размерности бросается NonPositiveDimensionException
 * Свойство index - минимальный индекс с некорректным значением, value - само значение
 *
 * Сама коллекция параметров недоступна, доступ - через методы интерфейса
 */
class DefaultShape(private vararg val dimensions: Int): Shape {
    init {
        if (dimensions.isEmpty()) {
            throw ShapeArgumentException.EmptyShapeException()
        }

        if (dimensions.any{it <= 0}) {
            val index = dimensions.indexOfFirst { it <= 0 }
            throw ShapeArgumentException.NonPositiveDimensionException(index, dimensions[index])
        }
    }
    override val ndim: Int
        get() = dimensions.size

    override fun dim(i: Int): Int = dimensions[i]

    override val size: Int by lazy { dimensions.fold(1) {base, cur -> base * cur} }
}

sealed class ShapeArgumentException (reason: String = "") : IllegalArgumentException(reason) {
    class EmptyShapeException: ShapeArgumentException("empty shape is not possible")

    class NonPositiveDimensionException(index: Int, value: Int):
        ShapeArgumentException("At position: $index found: $value")
}
