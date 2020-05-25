package io.github.takusan23.kaisendonmk2.TimeLine

/**
 * エスケープ文字の改行「\n」をHTMLの改行「brタグ」に置換する関数
 * */
internal fun String.escapeToBrTag(): String = this.replace("\\n", "<br>")