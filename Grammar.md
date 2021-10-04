<BlockItem>, <Decl>, <BType>

```
<CompUnit> ::= <Decl> {<FuncDef>} <MainFuncDef>  

<FuncDef> ::= <FuncType> <Ident> '(' [<FuncFParams>] ')' <Block> 
<MainFuncDef> ::= 'int' 'main' '(' ')' <Block> 				// 存在main函数
<FuncType> ::= 'void' | 'int' 								// 覆盖两种类型的函数
<FuncFParams> ::= <FuncFParam> { ',' <FuncFParam> } 
<FuncFParam> ::= <BType> <Ident> ['[' ']' { '[' ConstExp ']' }] 
// 1.普通变量  2.⼀维数组变量 3.⼆维数组变量

<Block> ::= '{' { <BlockItem> } '}' 
<BlockItem> ::= <Decl> | <Stmt> 							// 覆盖两种语句块项
<Decl> ::= <ConstDecl> | <VarDecl>							 // 覆盖两种声明
<ConstDecl> ::= 'const' BType <ConstDef> { ,<ConstDef> } ';' 
<ConstDef> ::= <Ident> { '[' <ConstExp> ']' } '=' <ConstInitVal> // 普通变量、⼀维数组、⼆维数组
<ConstInitVal> ::= <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}' 
<VarDecl> ::= <BType> <VarDef> {,<VarDef>} ';'
<VarDef> ::= <Ident> { '[' <ConstExp> ']' } 
// 包含普通变量、⼀维数组、⼆维数组定义。标识符<Ident>：| <Ident> { '[' <ConstExp> ']' } '=' <InitVal>
<InitVal> ::= <Exp> | '{' [ <InitVal> {,<InitVal>} ] '}'
<BType> ::= 'int' 											// 存在即可

<Stmt> ::= <LVal> '=' <Exp> ';' 							// 每种类型的语句都要覆盖
 | [Exp] ';' //有⽆Exp两种情况
 | <Block>
 | 'if' '( <Cond> ')' <Stmt> [ 'else' <Stmt> ] 
 | 'while' '(' <Cond> ')' <Stmt>
 | 'break;' | 'continue;' 
 | 'return' [<Exp>] ';' 
 | <LVal> = 'getint();'
 | 'printf('FormatString{,<Exp>}');' 


<Cond> ::= <LOrExp> 
<LOrExp> ::= <LAndExp> | <LOrExp> '||' <LAndExp>			// 1.LAndExp 2.|| 均需覆盖
<LAndExp> ::= <EqExp> | <LAndExp> && <EqExp> 				// 1.EqExp 2.&& 均需覆盖
<EqExp> ::= <RelExp> | <EqExp> (== | !=) <RelExp> // 1.RelExp 2.== 3.!=
<RelExp> ::= <AddExp> | <RelExp> (< | > | <= | >=) <AddExp> // 1.AddExp 2.< 3.> 4.<= 5.>= 
<ConstExp> ::= <AddExp> 									//TODO 使⽤的Ident 必须是常量
<Exp> ::= <AddExp>
<AddExp> ::= <MulExp> | <AddExp> (+|−) <MulExp> 			// 1.MulExp 2.+ 需覆盖 3.-需覆盖
<MulExp> ::= <UnaryExp> | <MulExp> (*|/|%) <UnaryExp> 		//1.UnaryExp 2.* 3./ 4.% 均需覆盖
<UnaryExp> ::= <PrimaryExp> | <Ident> '(' [<FuncRParams>] ')' | <UnaryOp> <UnaryExp>		
// 3种情况均需覆盖,函数调⽤也需要覆盖FuncRParams的不同情况
<PrimaryExp> ::= '(' <Exp> ')' | <LVal> | <Number>			 // 三种情况均需覆盖
<LVal> ::= <Ident> {'[' <Exp> ']'}						 	//1.普通变量 2.⼀维数组 3.⼆维数组
<FuncRParams> → <Exp> { ',' <Exp> } 						//Exp需要覆盖数组传参和部分数组传参
<ConstExp> ::= <Exp>  									// 使⽤的Ident 必须是常量, 存在即可
<UnaryOp> ::= + | - | ! 									// '!'仅出现在条件表达式中 
<Number> ::= <IntConst>										// 存在即可


```

| 单词名称         | 类别码     | 单词名称 | 类别码   | 单词名称 | 类别码 | 单词名称 | 类别码  |
| ---------------- | ---------- | -------- | -------- | -------- | ------ | -------- | ------- |
| **Ident**        | IDENFR     | !        | NOT      | *        | MULT   | =        | ASSIGN  |
| **IntConst**     | INTCON     | &&       | AND      | /        | DIV    | ;        | SEMICN  |
| **FormatString** | STRCON     | \|\|     | OR       | %        | MOD    | ,        | COMMA   |
| main             | MAINTK     | while    | WHILETK  | <        | LSS    | (        | LPARENT |
| const            | CONSTTK    | getint   | GETINTTK | <=       | LEQ    | )        | RPARENT |
| int              | INTTK      | printf   | PRINTFTK | >        | GRE    | [        | LBRACK  |
| break            | BREAKTK    | return   | RETURNTK | >=       | GEQ    | ]        | RBRACK  |
| continue         | CONTINUETK | +        | PLUS     | ==       | EQL    | {        | LBRACE  |
| if               | IFTK       | -        | MINU     | !=       | NEQ    | }        | RBRACE  |
| else             | ELSETK     | void     | VOIDTK   |          |        |          |         |