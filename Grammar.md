



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
 | [Exp] ';' //有无⽆Exp两种情况
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


<BlockItem>, <Decl>, <BType>
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



| **错误类型**                         | **错误类别码** | 解释                                                         | 对应文法及出错符号(…省略该条规则后续部分)                    |
| ------------------------------------ | -------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 非法符号                              | a              | 格式字符串中出现非法字符报错行号为**<FormatString>**所在行数。 | <FormatString> → ‘“‘{<Char>}’”’                              |
| 名字重定义                            | b              | 函数名或者变量名在**当前作用域**下重复定义。注意，变量一定是同一级作用域下才会判定出错，不同级作用域下，内层会覆盖外层定义。报错行号为**<Ident>**所在行数。 | <ConstDef>→<Ident> …<VarDef>→<Ident> … \|<Ident> … <FuncDef>→<FuncType><Ident> …<FuncFParam> → <BType> <Ident> ... |
| 未定义的名字                          | c              | 使用了未定义的标识符报错行号为**<Ident>**所在行数。          | <LVal>→<Ident> …<UnaryExp>→<Ident> …                         |
| 函数参数个数不匹配                     | d              | 函数调用语句中，参数个数与函数定义中的参数个数不匹配。报错行号为函数调用语句的**函数名**所在行数。 | <UnaryExp>→<Ident>‘(’[FuncRParams ]‘)’                       |
| 函数参数类型不匹配                     | e              | 函数调用语句中，参数类型与函数定义中对应位置的参数类型不匹配。报错行号为函数调用语句的**函数名**所在行数。 | <UnaryExp>→<Ident>‘(’[FuncRParams ]‘)’                       |
| 无返回值的函数存在不匹配的return语句     | f              | 报错行号为**‘return’**所在行号。                             | <Stmt>→‘return’ {‘[’Exp’]’}‘;’                               |
| 有返回值的函数缺少return语句         | g              | 只需要考虑函数末尾是否存在return语句，**无需考虑数据流**。报错行号为函数**结尾的****’}’**所在行号。 | FuncDef → FuncType Ident ‘(’ [FuncFParams] ‘)’ Block         |
| 不能改变常量的值                     | h              | <LVal>为常量时，不能对其修改。报错行号为**<LVal>**所在行号。 | <Stmt>→<LVal>‘=’ <Exp>‘;’\|<LVal>‘=’ ‘getint’ ‘(’ ‘)’ ‘;’    |
| 缺少分号                             | i              | 报错行号为分号**前一个非终结符**所在行号。                   | <Stmt>,<ConstDecl>及<VarDecl>中的';’                         |
| 缺少右小括号’)’                      | j              | 报错行号为右小括号**前一个非终结符**所在行号。               | 函数调用(<UnaryExp>)、函数定义(<FuncDef>)及<Stmt>中的')’     |
| 缺少右中括号’]’                      | k              | 报错行号为右中括号**前一个非终结符**所在行号。               | 数组定义(<ConstDef>,<VarDef>,<FuncFParam>)和使用(<LVal>)中的']’ |
| printf中格式字符与表达式个数不匹配   | l              | 报错行号为**‘printf’**所在行号。                             | Stmt →‘printf’‘(’FormatString{,Exp}’)’‘;’                    |
| 在非循环块中使用break和continue语句  | m              | 报错行号为**‘break’****与’continue’**所在行号。              | <Stmt>→‘break’‘;’\|‘continue’‘;’                             |



