package article.entities

import kotlin.test.*

class ContentBlockTest {

    private lateinit var blockList: MutableList<ContentBlock>

    @BeforeTest
    fun setup() {
        blockList = mutableListOf()
    }

    @Test
    fun add() {
        val block = TextBlock(text = "Hello")
        val result = blockList.addContentBlock(block)
        assertTrue(result)
        assertEquals(1, blockList.size)
        assertEquals(block, blockList[0])
    }

    @Test
    fun remove() {
        val block = TextBlock(text = "Remove")
        blockList.addContentBlock(block)

        val result = blockList.removeContentBlock(block)
        assertTrue(result)
        assertTrue(blockList.isEmpty())
    }

    @Test
    fun textCopy() {
        val original = TextBlock(text = "Copy")
        val copy = original.copyBlock() as TextBlock

        assertEquals(original.text, copy.text)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.PLAINTEXT, copy.blockType)
    }

    @Test
    fun markdownCopy() {
        val original = MarkdownBlock(text = "## Heading")
        val copy = original.copyBlock() as MarkdownBlock

        assertEquals(original.text, copy.text)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.MARKDOWN, copy.blockType)
    }

    @Test
    fun codeBlockCopy() {
        val original = CodeBlock(text = "println(\"Hello\")", language = "kotlin")
        val copy = original.copyBlock() as CodeBlock

        assertEquals(original.text, copy.text)
        assertEquals(original.language, copy.language)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.CODE, copy.blockType)
    }

    @Test
    fun canvasBlockCopy() {
        val original = CanvasBlock(bList = mutableListOf(1, 2, 3), canvasHeight = 300)
        val copy = original.copyBlock() as CanvasBlock

        assertEquals(original.bList, copy.bList)
        assertEquals(original.canvasHeight, copy.canvasHeight)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.CANVAS, copy.blockType)
    }

    @Test
    fun mathBlockCopy() {
        val original = MathBlock(text = "\\frac{a}{b}")
        val copy = original.copyBlock() as MathBlock

        assertEquals(original.text, copy.text)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.MATH, copy.blockType)
    }

    @Test
    fun mediaBlockCopy() {
        val original = MediaBlock(bList = mutableListOf(4, 5, 6))
        val copy = original.copyBlock() as MediaBlock

        assertEquals(original.bList, copy.bList)
        assertNotEquals(original.id, copy.id)
        assertEquals(BlockType.MEDIA, copy.blockType)
    }

    @Test
    fun create() {
        val plain = BlockType.PLAINTEXT.createDefaultBlock()
        val markdown = BlockType.MARKDOWN.createDefaultBlock()
        val code = BlockType.CODE.createDefaultBlock()
        val canvas = BlockType.CANVAS.createDefaultBlock()
        val math = BlockType.MATH.createDefaultBlock()
        val media = BlockType.MEDIA.createDefaultBlock()

        assertTrue(plain is TextBlock)
        assertTrue(markdown is MarkdownBlock)
        assertTrue(code is CodeBlock)
        assertTrue(canvas is CanvasBlock)
        assertTrue(math is MathBlock)
        assertTrue(media is MediaBlock)
    }
}
