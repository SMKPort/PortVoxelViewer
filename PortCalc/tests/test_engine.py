import json
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'app'/'src'/'main'/'python'))
from engine import solve

class EngineTests(unittest.TestCase):
    def check(self, operation, args, expected):
        result=json.loads(solve({'op':operation,'args':args}))
        self.assertTrue(result['ok'], result)
        self.assertEqual(result['answer'],expected)
        self.assertTrue(result['steps'])
        return result

    def test_limit(self): self.check('limit',{'f':'sin(x)/x','point':'0'},'1')
    def test_lateral_limit(self): self.check('limit',{'f':'1/x','point':'0','side':'+'},'oo')
    def test_derivative(self): self.check('derivative',{'f':'x^3','order':'2'},'6*x')
    def test_tangent(self): self.check('tangent',{'f':'x^2','point':'2'},'Eq(Y, 4*x - 4)')
    def test_roots(self): self.check('roots',{'f':'x^2-4'},'{-2, 2}')
    def test_integral(self): self.check('integral',{'f':'2*x'},'C + x^2')
    def test_def_integral(self): self.check('def_integral',{'f':'x^2','a':'0','b':'3'},'9')
    def test_area(self): self.check('area',{'f':'x','g':'x^2','a':'0','b':'1'},'1/6')
    def test_volume(self): self.check('volume',{'f':'x','a':'0','b':'1'},'pi/3')
    def test_taylor(self): self.check('taylor',{'f':'exp(x)','center':'0','order':'2'},'x^2/2 + x + 1')
    def test_ode1(self): self.check('ode1',{'p':'1','q':'0'},'Eq(y, C*exp(-x))')
    def test_partial(self): self.check('partial',{'f':'x^2*y','var':'x'},'2*x*y')
    def test_gradient(self): self.check('gradient',{'f':'x^2+y^2'},'Matrix([[2*x], [2*y]])')
    def test_direction(self): self.check('direction',{'f':'x^2+y^2','x0':'1','y0':'2','u':'3','v':'4'},'22/5')
    def test_double(self): self.check('double',{'f':'x+y','a':'0','b':'1','c':'0','d':'2'},'3')
    def test_triple(self): self.check('triple',{'f':'x+y+z','a':'0','b':'1','c':'0','d':'1','e':'0','h':'1'},'3/2')
    def test_divergence(self): self.check('divergence',{'P':'x^2','Q':'y^2','R':'z^2'},'2*x + 2*y + 2*z')
    def test_curl(self): self.check('curl',{'P':'-y','Q':'x','R':'0'},'Matrix([[0], [0], [2]])')
    def test_laplace(self): self.check('laplace',{'f':'exp(-2*t)'},'1/(s + 2)')
    def test_inv_laplace(self): self.check('inv_laplace',{'f':'1/(s+2)'},'exp(-2*t)*Heaviside(t)')
    def test_fourier(self):
        r=json.loads(solve({'op':'fourier','args':{'f':'x','order':'2'}}))
        self.assertTrue(r['ok'],r)
        self.assertTrue('sin' in r['answer'])
    def test_ode2(self):
        r=json.loads(solve({'op':'ode2','args':{'a':'1','b':'3','c':'2'}}))
        self.assertTrue(r['ok'],r)
        self.assertIn('C1',r['answer'])
    def test_line(self): self.check('line',{'P':'-y','Q':'x','xt':'cos(t)','yt':'sin(t)','a':'0','b':'2*pi'},'2*pi')
    def test_graph(self):
        r=self.check('graph',{'f':'sin(x)','var':'x','a':'-4','b':'4'},'sin(x)')
        self.assertGreater(len(r['plot']['points']),200)
    def test_reject_code(self):
        result=json.loads(solve({'op':'derivative','args':{'f':'__import__("os")'}}))
        self.assertFalse(result['ok'])
    def test_reject_unknown(self):
        result=json.loads(solve({'op':'derivative','args':{'f':'foo(x)'}}))
        self.assertFalse(result['ok'])
    def test_reject_invalid_direction(self):
        result=json.loads(solve({'op':'direction','args':{'f':'x+y','x0':'0','y0':'0','u':'0','v':'0'}}))
        self.assertFalse(result['ok'])

if __name__=='__main__':unittest.main()
