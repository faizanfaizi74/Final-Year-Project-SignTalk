import flask
from flask import jsonify
import werkzeug
import numpy as np

import model_file as mf  # working code file

app = flask.Flask(__name__)

@app.route('/', methods=['GET', 'POST'])
def index():
    return "Sign Talk Mobile App"

@app.route('/predict', methods=['POST'])
def handle_request():
    # data from app
    videofile = flask.request.files['video']  # image0 to video

    filename = werkzeug.utils.secure_filename(videofile.filename)
    print("\nReceived Video File Name : " + videofile.filename)
    videofile.save(filename)

    probabilities = mf.sign_prediction(filename)

    v = ['Call the ambulance!', 'I am a student.', 'I can not speak', 'How are you?', "I don't understand"]
    pred_class = v[np.argmax(probabilities)]

    
    return jsonify({'Sentence': str(pred_class)})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)