package org.jetbrains.plugins.scala.lang.completion.lookups

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import com.intellij.util.ui.EmptyIcon
import org.jetbrains.plugins.scala.lang.completion.ScalaKeyword
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementPresentation, LookupItem}
import org.jetbrains.plugins.scala.ScalaFileType
import com.intellij.psi.{PsiDocumentManager, PsiElement}
import com.intellij.psi.codeStyle.{CodeStyleManager, CodeStyleSettingsManager}
import com.intellij.openapi.util.TextRange

/**
 * @author Alefas
 * @since 27.03.12
 */

class ScalaKeywordLookupItem(val keyword: String, position: PsiElement) extends {
  val keywordPsi: PsiElement = new ScalaLightKeyword(position.getManager, keyword)
} with LookupItem[PsiElement](keywordPsi, keyword) {
  override def hashCode(): Int = keyword.hashCode

  override def equals(o: Any): Boolean = {
    o match {
      case s: ScalaKeywordLookupItem => s.keyword == keyword
      case _ => false
    }
  }

  override def renderElement(presentation: LookupElementPresentation) {
    presentation.setItemText(keyword)
    presentation.setItemTextBold(true)
    presentation.setIcon(new EmptyIcon(16, 16))
  }

  import ScalaKeyword._
  val expressions = Set(THIS, FALSE, TRUE, NULL, SUPER)
  val parentheses = Set(IF, FOR, WHILE)
  val braces = Set(CATCH, ELSE, EXTENDS, FINALLY, FOR, FOR_SOME, NEW, TRY, DO, YIELD)

  override def handleInsert(context: InsertionContext) {
    val editor = context.getEditor
    val document = editor.getDocument
    val offset = context.getStartOffset + keyword.length
    keyword match {
      case THIS | FALSE | TRUE | NULL | SUPER => // do nothing
      case _ => {
        def addSpace(addCompletionChar: Boolean = false) {
          context.setAddCompletionChar(addCompletionChar)
          if (document.getTextLength <= offset || document.getText.charAt(offset) != ' ')
            document.insertString(offset, " ")
          editor.getCaretModel.moveToOffset(offset + 1)
        }
        val settings = CodeStyleSettingsManager.getInstance(context.getProject).getCurrentSettings.getCommonSettings(ScalaFileType.SCALA_LANGUAGE)
        context.getCompletionChar match {
          case '(' if parentheses.contains(keyword) =>
            val add = keyword match {
              case IF => settings.SPACE_BEFORE_IF_PARENTHESES
              case FOR => settings.SPACE_BEFORE_FOR_PARENTHESES
              case WHILE => settings.SPACE_BEFORE_WHILE_PARENTHESES
            }
            if (add) addSpace(true)
          case '{' if braces.contains(keyword) =>
            val add = keyword match {
              case CATCH => settings.SPACE_BEFORE_CATCH_LBRACE
              case ELSE => settings.SPACE_BEFORE_ELSE_LBRACE
              case EXTENDS => true
              case FINALLY => settings.SPACE_BEFORE_FINALLY_LBRACE
              case FOR => settings.SPACE_BEFORE_FOR_LBRACE
              case FOR_SOME => true
              case NEW => true
              case TRY => settings.SPACE_BEFORE_TRY_LBRACE
              case DO => settings.SPACE_BEFORE_DO_LBRACE
              case YIELD => settings.SPACE_BEFORE_FOR_LBRACE
            }
            if (add) addSpace(true)
          case _ => addSpace()
        }
        if (keyword == CASE) {
          val manager = PsiDocumentManager.getInstance(context.getProject)
          manager.commitDocument(document)
          val file = manager.getPsiFile(document)
          if (file == null) return
          CodeStyleManager.getInstance(context.getProject).
            adjustLineIndent(file, new TextRange(context.getStartOffset, offset))
        }
      }
    }
  }
}
