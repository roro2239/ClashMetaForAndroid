package com.github.kr328.clash

fun Set<String>.decodeAppRules(): Map<String, AccessControlComposeDesign.AppRule> {
    return mapNotNull { item ->
        val packageName = item.substringBefore('|')
        val ruleName = item.substringAfter('|', "")
        val rule = AccessControlComposeDesign.AppRule.entries.find { it.name == ruleName }

        if (packageName.isBlank() || rule == null || rule == AccessControlComposeDesign.AppRule.Default) {
            null
        } else {
            packageName to rule
        }
    }.toMap()
}

fun Map<String, AccessControlComposeDesign.AppRule>.encodeAppRules(): Set<String> {
    return mapNotNull { (packageName, rule) ->
        if (packageName.isBlank() || rule == AccessControlComposeDesign.AppRule.Default) {
            null
        } else {
            "$packageName|${rule.name}"
        }
    }.toSet()
}
