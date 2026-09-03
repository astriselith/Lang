include("./control-flow");

println("
Preencha algumas informações:

requeridos: nome e idade e ser maior de idade.
");

read = (message, type) -> {
	finalize("return");
	return = (val) -> launch("return", val);

	# body #
    print(message);
    value = readln(type);
    return(value);
};

name = read("Nome: ", "string");
age = read("Idade: ", "int");

errors = listOf();

if(name == ""): {
	errors.add("Nome não informado");
};
if(age == 0): {
	errors.add("Idade não informada.");
};

if(age < 18): {
	errors.add("Idade minima não atingida");
};

println();
if(errors.isEmpty()): {
	println("Completo: ");
	println("Name: @name");
	println("Age: @age");
} | {
	println("Erros encontrados no cadastro:");
	i = 0;
	while(i < errors.size()): {
		println(errors.get(i));
		i = i + 1;
	};
	println("Não foi possivel concluir o cadastro.");
};
