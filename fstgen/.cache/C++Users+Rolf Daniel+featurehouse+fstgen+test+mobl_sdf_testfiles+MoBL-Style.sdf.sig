module(unparameterized("MoBL-Style"),[imports([module(unparameterized("Common")),module(unparameterized("MoBL"))])],[exports(conc-grammars(conc-grammars(conc-grammars(context-free-syntax([prod([iter-star(sort("MetaAnno")),lit("\"style\""),iter(sort("StyleSelector")),lit("\"{\""),iter-star(sort("StyleElem")),lit("\"}\"")],sort("Definition"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"Style\""))])))])),prod([iter-star(sort("MetaAnno")),lit("\"style\""),lit("\"mixin\""),sort("QId"),lit("\"(\""),iter-star-sep(sort("StyleFArg"),lit("\",\"")),lit("\")\""),lit("\"{\""),iter-star(sort("StyleElem")),lit("\"}\"")],sort("Definition"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleMixin\""))])))])),prod([lit("\"$\""),sort("STYLEID")],sort("StyleFArg"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleFArg\""))])))])),prod([lit("\"style\""),sort("StyleVar"),lit("\"=\""),sort("StyleExp")],sort("Definition"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleVarDecl\""))])))])),prod([lit("\"$\""),sort("STYLEID")],sort("StyleVar"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleVar\""))])))])),prod([iter-star(sort("MetaAnno")),lit("\"external\""),lit("\"style\""),iter(sort("StyleSelector"))],sort("Definition"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"ExternalStyle\""))])))])),prod([sort("STYLEPROPID"),lit("\":\""),sort("StyleExpsTL"),lit("\";\"")],sort("StyleElem"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleProperty\""))])))])),prod([sort("QId"),lit("\"(\""),iter-star-sep(sort("StyleExps"),lit("\",\"")),lit("\")\""),lit("\";\"")],sort("StyleElem"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleMixinCall\""))])))])),prod([sort("QId"),lit("\";\"")],sort("StyleElem"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleElemRecover\""))]))),avoid])),prod([sort("QId")],sort("StyleSelector"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleSelector\""))])))])),prod([sort("QId"),lit("\":\""),sort("STYLEPROPID")],sort("StyleSelector"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleSelectorWithPseudo\""))])))])),prod([sort("QId"),lit("\":\""),sort("STYLEPROPID"),lit("\"(\""),sort("STYLEPROPID"),lit("\")\"")],sort("StyleSelector"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleSelectorWithPseudoArg\""))])))])),prod([sort("COLOR")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"Color\""))])))])),prod([sort("STYLEID")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleId\""))])))])),prod([sort("STRING")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"String\""))])))])),prod([sort("NUMBER")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"Num\""))])))])),prod([sort("NUMBER"),sort("Unit")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"NumUnit\""))])))])),prod([sort("StyleVar")],sort("StyleExp"),no-attrs),prod([sort("StyleExp"),lit("\"+\""),sort("StyleExp")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleAdd\""))]))),assoc(left)])),prod([sort("StyleExp"),lit("\"~\""),sort("StyleExp")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleSub\""))]))),assoc(left)])),prod([sort("StyleExp"),lit("\"*\""),sort("StyleExp")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleMul\""))]))),assoc(left)])),prod([sort("StyleVar"),lit("\".\""),sort("ID")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleFieldAccess\""))])))])),prod([lit("\"$data\""),lit("\"(\""),sort("Path"),lit("\")\"")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"ImportData\""))])))])),prod([sort("FILENAME"),lit("\"/\""),iter-sep(sort("FILENAME"),lit("\"/\""))],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StylePath\""))]))),avoid])),prod([sort("STYLEPROPID"),lit("\"(\""),iter-star-sep(sort("StyleExps"),lit("\",\"")),lit("\")\"")],sort("StyleExp"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleCall\""))])))])),prod([iter(sort("StyleExp"))],sort("StyleExps"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleExps\""))]))),prefer])),prod([iter-sep(sort("StyleExp"),lit("\",\""))],sort("StyleExpsTL"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"StyleExpsCS\""))]))),prefer])),prod([sort("StyleExps")],sort("StyleExpsTL"),no-attrs),prod([lit("\"px\"")],sort("Unit"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"PxUnit\""))])))])),prod([lit("\"em\"")],sort("Unit"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"EmUnit\""))])))])),prod([lit("\"pt\"")],sort("Unit"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"PtUnit\""))])))])),prod([lit("\"s\"")],sort("Unit"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"SUnit\""))])))])),prod([lit("\"%\"")],sort("Unit"),attrs([term(default(appl(unquoted("cons"),[fun(quoted("\"PercentageUnit\""))])))]))]),context-free-priorities([chain([simple-group(prod([sort("StyleExp"),lit("\"*\""),sort("StyleExp")],sort("StyleExp"),no-attrs)),prods-group([prod([sort("StyleExp"),lit("\"+\""),sort("StyleExp")],sort("StyleExp"),no-attrs),prod([sort("StyleExp"),lit("\"~\""),sort("StyleExp")],sort("StyleExp"),no-attrs)])])])),lexical-syntax([prod([char-class(simple-charclass(present(short("\\#")))),iter-star(char-class(simple-charclass(present(conc(range(short("0"),short("9")),conc(range(short("a"),short("f")),range(short("A"),short("F"))))))))],sort("COLOR"),no-attrs),prod([char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(short("\\_"),short("\\-"))))))),iter(char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(range(short("0"),short("9")),conc(short("\\_"),short("\\-")))))))))],sort("STYLEPROPID"),no-attrs),prod([char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),short("\\_")))))),iter(char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(range(short("0"),short("9")),conc(short("\\_"),short("\\-")))))))))],sort("STYLEID"),no-attrs),prod([lit("\"px\"")],sort("STYLEID"),attrs([reject])),prod([lit("\"em\"")],sort("STYLEID"),attrs([reject])),prod([lit("\"pt\"")],sort("STYLEID"),attrs([reject])),prod([lit("\"mixin\"")],sort("ID"),attrs([reject]))])),lexical-restrictions([follow([sort("STYLEID")],single(char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(range(short("0"),short("9")),short("\\_"))))))))),follow([sort("STYLEPROPID")],single(char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(range(short("0"),short("9")),short("\\_"))))))))),follow([sort("COLOR")],single(char-class(simple-charclass(present(conc(range(short("a"),short("z")),conc(range(short("A"),short("Z")),conc(range(short("0"),short("9")),conc(short("\\_"),short("\\#"))))))))))])))])