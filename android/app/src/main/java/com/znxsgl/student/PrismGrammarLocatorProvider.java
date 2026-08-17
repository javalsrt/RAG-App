package com.znxsgl.student;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Prism4j 语法定位器生成入口。
 * 编译时由 prism4j-bundler 根据 @PrismBundle 注解生成 GrammarLocator 类。
 */
@PrismBundle(
        include = {"python", "java", "kotlin", "javascript", "json", "sql"},
        grammarLocatorClassName = ".PrismGrammarLocator"
)
public class PrismGrammarLocatorProvider {
}
