"""PortCalc: motor simbólico local para Cálculo I-IV (SymPy).
Entrada/saída JSON para ponte Android WebView/Chaquopy.
"""
import json
import math
import re
from sympy import (
    Symbol, symbols, sin, cos, tan, asin, acos, atan, sinh, cosh, tanh,
    sqrt, exp, log, Abs, pi, E, I, oo, factorial, sign, Heaviside,
    integrate, diff, limit, solve, solveset, S, simplify, factor, expand,
    series, Eq, Matrix, Integral, Derivative, Function, dsolve, N,
    laplace_transform, inverse_laplace_transform, lambdify,
    sstr, latex, Add, Mul, Pow, Float
)
from sympy.parsing.sympy_parser import (
    parse_expr, standard_transformations, implicit_multiplication_application,
    convert_xor
)

x, y, z, t, s, u, v = symbols('x y z t s u v', real=True)
TRANSFORMS = standard_transformations + (implicit_multiplication_application, convert_xor)
SYMS = {k: symbol for k, symbol in zip(('x','y','z','t','s','u','v'), (x,y,z,t,s,u,v))}
ALLOWED = {
    **SYMS, 'pi': pi, 'E': E, 'e': E, 'I': I, 'oo': oo,
    'sin': sin, 'cos': cos, 'tan': tan, 'asin': asin, 'acos': acos,
    'atan': atan, 'sinh': sinh, 'cosh': cosh, 'tanh': tanh,
    'sqrt': sqrt, 'exp': exp, 'ln': log, 'log': log, 'abs': Abs,
    'Abs': Abs, 'factorial': factorial, 'sign': sign,
    'Heaviside': Heaviside,
}
# Com parser em whitelist, letras não autorizadas são rejeitadas antes de parse_expr.
# Limitar também potenciais custos altos por chamadas repetidas.
MAX_INPUT = 180

def expr(raw):
    value = str(raw).strip()
    if not value or len(value) > MAX_INPUT:
        raise ValueError('Digite uma expressão de até 180 caracteres.')
    if not re.fullmatch(r'[A-Za-z0-9+*/^().,\- \t]+', value):
        raise ValueError('Use somente números, funções, parênteses e operações matemáticas.')
    for name in re.findall(r'[A-Za-z]+', value):
        if name not in ALLOWED and not (len(name) > 1 and all(c in SYMS for c in name)):
            raise ValueError('Símbolo não permitido: ' + name)
    try:
        result = parse_expr(value, local_dict=ALLOWED.copy(),
                            global_dict={'__builtins__': {}, 'Symbol': Symbol,
                                         'Integer': __import__('sympy').Integer,
                                         'Float': Float,
                                         'Rational': __import__('sympy').Rational,
                                         'factorial': factorial},
                            transformations=TRANSFORMS, evaluate=True)
    except Exception as ex:
        raise ValueError('Não foi possível interpretar a expressão. Confira a sintaxe.') from ex
    if not hasattr(result, 'free_symbols'):
        raise ValueError('Expressão inválida.')
    return result

def var(raw, default='x'):
    name = str(raw or default)
    if name not in SYMS:
        raise ValueError('Variável inválida. Use x, y, z, t, s, u ou v.')
    return SYMS[name]

def integer(raw, default=1, minimum=1, maximum=12):
    try:
        n = int(raw if raw is not None else default)
    except (TypeError, ValueError) as ex:
        raise ValueError('Informe um número inteiro válido.') from ex
    if n < minimum or n > maximum:
        raise ValueError(f'Use um inteiro entre {minimum} e {maximum}.')
    return n

def fmt(value):
    return str(value).replace('**', '^')

def step(title, mathematical=None, detail=''):
    return {'title': title, 'math': fmt(mathematical) if mathematical is not None else '',
            'detail': detail}

def response(title, result, steps, extra=None, plot=None, warning=None):
    data = {'ok': True, 'title': title, 'answer': fmt(result),
            'latex': latex(result), 'steps': steps}
    if extra:
        data['extra'] = extra
    if plot:
        data['plot'] = plot
    if warning:
        data['warning'] = warning
    return data

def num(raw):
    value = expr(raw)
    if value.free_symbols:
        raise ValueError('Este campo precisa ser numérico ou uma constante, como pi.')
    return value

def real_value(val):
    try:
        c = complex(N(val, 12))
        if math.isfinite(c.real) and abs(c.imag) < 1e-8:
            return c.real
    except Exception:
        pass
    return None

def sample_plot(f, variable=x, xmin=-8, xmax=8):
    try:
        func = lambdify(variable, f, modules=['math'])
        points = []
        for i in range(241):
            xi = xmin + (xmax-xmin)*i/240
            try:
                yi = float(func(xi))
                if not math.isfinite(yi) or abs(yi) > 300:
                    points.append(None)
                else:
                    points.append([round(xi,5), round(yi,5)])
            except (ValueError, ZeroDivisionError, OverflowError, TypeError):
                points.append(None)
        return {'points': points, 'xlabel': str(variable), 'ylabel': 'f('+str(variable)+')'}
    except Exception:
        return None

def solve_operation(request):
    op = str(request.get('op','')).strip()
    args = request.get('args') or {}
    get = lambda name, default='': str(args.get(name, default)).strip()
    if op == 'limit':
        f, vv, p = expr(get('f')), var(get('var')), expr(get('point'))
        side = get('side','+-')
        if side not in ('+', '-', '+-'):
            raise ValueError('Escolha limite bilateral, pela esquerda ou pela direita.')
        result = limit(f, vv, p, dir=side)
        return response('Limite', result, [step('Função de partida', f),
            step('Aproximação', Eq(vv,p), 'Lado: ' + {'+':'direita','-':'esquerda','+-':'ambos'}[side]),
            step('Limite obtido', result)], plot=sample_plot(f,vv))
    if op == 'derivative':
        f, vv = expr(get('f')), var(get('var'))
        n = integer(get('order',1), 1, 1, 5)
        result = diff(f, vv, n)
        return response('Derivada', result, [step('Expressão original', f),
            step(f'Aplicando derivação {n} vez(es) em {vv}', Derivative(f, (vv,n))),
            step('Simplificação', simplify(result), 'Derivada simbólica conferida pelo motor.')],
            plot=sample_plot(result,vv))
    if op == 'tangent':
        f, vv, x0 = expr(get('f')), var(get('var')), num(get('point'))
        slope = simplify(diff(f,vv).subs(vv,x0))
        fx = simplify(f.subs(vv,x0))
        result = Eq(Symbol('Y'), simplify(slope*(vv-x0)+fx))
        return response('Reta tangente', result,
                        [step('Valor da função no ponto', fx), step('Inclinação f\'(a)', slope),
                         step('Equação da reta tangente', result)], plot=sample_plot(f,vv))
    if op == 'roots':
        f, vv = expr(get('f')), var(get('var'))
        result = solveset(f, vv, domain=S.Reals)
        return response('Raízes reais', result,
                        [step('Igualando a função a zero', Eq(f,0)),
                         step('Conjunto solução real', result)], plot=sample_plot(f,vv))
    if op == 'integral':
        f, vv = expr(get('f')), var(get('var'))
        F = integrate(f,vv)
        C = Symbol('C')
        result = F + C
        warning = 'O motor não conseguiu uma primitiva fechada.' if F.has(Integral) else None
        return response('Integral indefinida', result,
            [step('Integral solicitada', Integral(f,vv)),
             step('Primitiva calculada', F),
             step('Conferência pela derivada', simplify(diff(F,vv)))], warning=warning)
    if op == 'def_integral':
        f, vv, a, b = expr(get('f')), var(get('var')), expr(get('a')), expr(get('b'))
        F = integrate(f,vv)
        result = integrate(f,(vv,a,b))
        return response('Integral definida', result,
            [step('Integral solicitada', Integral(f,(vv,a,b))),
             step('Primitiva candidata', F), step('Aplicando os limites de integração',result)])
    if op == 'area':
        f, g, a,b = expr(get('f')),expr(get('g')),num(get('a')),num(get('b'))
        if a == b:
            raise ValueError('Escolha um intervalo com largura diferente de zero.')
        low, high = sorted([a,b], key=lambda w: float(N(w)))
        d = simplify(f-g)
        result = integrate(Abs(d),(x,low,high))
        if result.has(Integral):
            result = N(Integral(Abs(d),(x,low,high)),8)
        return response('Área entre curvas', result,
            [step('Diferença entre funções', d),
             step('Integral do módulo no intervalo', Integral(Abs(d),(x,low,high))),
             step('Área calculada',result)], plot=sample_plot(d))
    if op == 'volume':
        f,a,b = expr(get('f')),num(get('a')),num(get('b'))
        result = simplify(pi*integrate(f**2,(x,a,b)))
        return response('Volume de revolução',result,
           [step('Método dos discos (rotação em torno do eixo x)',pi*f**2),
            step('Integração no intervalo',Integral(pi*f**2,(x,a,b))),
            step('Volume obtido',result)])
    if op == 'taylor':
        f,vv,c = expr(get('f')),var(get('var')),num(get('center','0'))
        n = integer(get('order',5),5,1,10)
        result = series(f,vv,c,n+1).removeO().expand()
        return response('Polinômio de Taylor',result,
            [step('Função analisada',f), step(f'Centro {fmt(c)} e grau {n}',Eq(vv,c)),
             step('Polinômio truncado',result)],plot=sample_plot(result,vv))
    if op == 'ode1':
        p,q = expr(get('p')),expr(get('q'))
        mu = exp(integrate(p,x))
        result = simplify((integrate(mu*q,x)+Symbol('C'))/mu)
        return response('EDO linear de 1ª ordem', Eq(Symbol('y'),result),
            [step("Modelo: y' + p(x)y = q(x)", Eq(Symbol('p'),p)),
             step('Fator integrante μ(x)',mu),
             step('Solução geral',Eq(Symbol('y'),result))])
    if op == 'partial':
        f,vv = expr(get('f')),var(get('var'))
        if vv not in (x,y,z):
            raise ValueError('Use x, y ou z para derivadas parciais.')
        result = diff(f,vv)
        return response('Derivada parcial',result,
            [step('Expressão multivariável',f),
             step(f'Derivando em {vv} e fixando as demais variáveis',Derivative(f,vv)),
             step('Resultado',result)])
    if op == 'gradient':
        f = expr(get('f'))
        vs = [x,y,z] if get('dimension','2') == '3' else [x,y]
        result = Matrix([diff(f,w) for w in vs])
        return response('Gradiente',result,
            [step('Campo escalar',f),
             *[step('Derivada em '+str(w),diff(f,w)) for w in vs],
             step('Vetor gradiente',result)])
    if op == 'direction':
        f = expr(get('f'))
        xv,yv = num(get('x0')),num(get('y0'))
        a,b = num(get('u')),num(get('v'))
        length = sqrt(a*a+b*b)
        if length == 0: raise ValueError('O vetor direção não pode ser nulo.')
        gg = Matrix([diff(f,x),diff(f,y)])
        at = gg.subs({x:xv,y:yv})
        unit = Matrix([a/length,b/length])
        result = simplify(at.dot(unit))
        return response('Derivada direcional',result,
           [step('Gradiente geral',gg),step('Gradiente no ponto',at),
            step('Direção unitária',unit),step('Produto escalar',result)])
    if op == 'double':
        f = expr(get('f'))
        a,b,c,d = [expr(get(k)) for k in ('a','b','c','d')]
        inner = integrate(f,(x,a,b))
        result = integrate(inner,(y,c,d))
        return response('Integral dupla',result,
          [step('Região de integração: x de a até b, y de c até d',
                Integral(f,(x,a,b),(y,c,d))),
           step('Integração interna em x',inner),step('Integração externa em y',result)])
    if op == 'triple':
        f = expr(get('f'))
        a,b,c,d,e,h = [expr(get(k)) for k in ('a','b','c','d','e','h')]
        inner = integrate(f,(x,a,b))
        middle = integrate(inner,(y,c,d))
        result = integrate(middle,(z,e,h))
        return response('Integral tripla',result,
            [step('Integrando primeiro em x',inner),
             step('Integrando depois em y',middle),
             step('Integrando por último em z',result)])
    if op == 'divergence':
        P,Q,R = [expr(get(k)) for k in ('P','Q','R')]
        terms = [diff(P,x),diff(Q,y),diff(R,z)]
        result = simplify(sum(terms))
        return response('Divergente',result,
           [step('∂P/∂x',terms[0]),step('∂Q/∂y',terms[1]),
            step('∂R/∂z',terms[2]),step('Soma das derivadas',result)])
    if op == 'curl':
        P,Q,R = [expr(get(k)) for k in ('P','Q','R')]
        result = Matrix([diff(R,y)-diff(Q,z),diff(P,z)-diff(R,x),diff(Q,x)-diff(P,y)])
        return response('Rotacional',result,
           [step('Campo vetorial',Matrix([P,Q,R])),
            step('∇ × F',result)])
    if op == 'laplace':
        f = expr(get('f'))
        result = laplace_transform(f,t,s,noconds=True)
        return response('Transformada de Laplace',result,
            [step('Função no tempo',f),step('Aplicando ℒ{f(t)}',result)])
    if op == 'inv_laplace':
        f = expr(get('f'))
        result = inverse_laplace_transform(f,s,t)
        return response('Transformada inversa de Laplace',result,
            [step('Função F(s)',f), step('Aplicando ℒ⁻¹{F(s)}',result)])
    if op == 'fourier':
        f = expr(get('f'))
        nmax = integer(get('order',3),3,1,6)
        nn = Symbol('n',integer=True,positive=True)
        a0 = simplify((1/pi)*integrate(f,(x,-pi,pi)))
        total = a0/2
        steps = [step('Função no intervalo [-π,π]',f),step('Coeficiente a₀',a0)]
        for k in range(1,nmax+1):
            ak = simplify((1/pi)*integrate(f*cos(k*x),(x,-pi,pi)))
            bk = simplify((1/pi)*integrate(f*sin(k*x),(x,-pi,pi)))
            total += ak*cos(k*x)+bk*sin(k*x)
            steps.append(step(f'Coeficientes para n={k}',Matrix([ak,bk]),'Primeiro aₙ, depois bₙ.'))
        total = simplify(total)
        steps.append(step('Soma parcial de Fourier',total))
        return response('Série de Fourier',total,steps,plot=sample_plot(total))
    if op == 'ode2':
        aa,bb,cc = [num(get(k)) for k in ('a','b','c')]
        if aa == 0:
            raise ValueError('O coeficiente a de y\'\' deve ser diferente de zero.')
        Y = Function('y')
        equation = Eq(aa*diff(Y(t),t,2)+bb*diff(Y(t),t)+cc*Y(t),0)
        result = dsolve(equation,Y(t))
        delta = simplify(bb**2-4*aa*cc)
        return response('EDO homogênea de 2ª ordem',result,
            [step('Equação diferencial',equation),
             step('Discriminante da característica',delta),
             step('Solução geral',result)])
    if op == 'line':
        P,Q,xt,yt = [expr(get(k)) for k in ('P','Q','xt','yt')]
        a,b = [num(get(k)) for k in ('a','b')]
        integrand = simplify(P.subs({x:xt,y:yt})*diff(xt,t)+Q.subs({x:xt,y:yt})*diff(yt,t))
        result = integrate(integrand,(t,a,b))
        return response('Integral de linha de campo vetorial',result,
             [step('Parametrização da curva',Matrix([xt,yt])),
              step('Produto F(r(t)) · r\'(t)',integrand),
              step('Integral no intervalo',Integral(integrand,(t,a,b))),
              step('Trabalho calculado',result)])
    if op == 'graph':
        f,vv = expr(get('f')),var(get('var'))
        a,b = num(get('a','-8')),num(get('b','8'))
        xmin,xmax = real_value(a),real_value(b)
        if xmin is None or xmax is None or xmin >= xmax:
            raise ValueError('Escolha limites reais com mínimo menor que o máximo.')
        if xmax-xmin > 10000: raise ValueError('Intervalo muito amplo para o gráfico.')
        plot = sample_plot(f,vv,xmin,xmax)
        if not plot: raise ValueError('Não foi possível desenhar esta função em números reais.')
        return response('Gráfico de função',f,[step('Função representada',f)],plot=plot)
    raise ValueError('Operação não reconhecida. Atualize o aplicativo.')

def solve(payload):
    try:
        request = json.loads(payload) if isinstance(payload,str) else payload
        return json.dumps(solve_operation(request),ensure_ascii=False)
    except Exception as ex:
        return json.dumps({'ok':False,'error':str(ex)[:250]},ensure_ascii=False)
