include("./control-flow");

println("
Preencha algumas informações:

requeridos: nome e idade.
");

read = (message) -> {
	finalize("return");

    print(message);
    line = readln();
	
    launch("return", line);
};

name = read("Nome: ");
age = read("Idade: ");

println();

try: {
	assert(name != "", "O nome não foi preenchido");
} :: (err) -> {
	println(err);
};

try: {
	assert(age != "", "A idade não foi preenchida");
} :: (err) -> {
	println(err);
};

println();
println("Completo");

println();




println();
println("Name: " + name);
println("Age: " + age);






