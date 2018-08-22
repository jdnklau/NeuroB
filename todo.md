# Todo's

## Refactoring

### Training sets

- [ ] Generation of training set
  - [ ] Training data formats
    - [ ] Loading of samples from the format
    - [ ] Appending to a format
  - [x] Create DB
  - [x] Create features and labels
  - [x] Skip already existing data
    - [x] naively by timestamp
    - [ ] _optional_ by versioned generation step
  - [ ] Generation statistics
    - [x] Return statistics after creation of data
    - [ ] Split DataGeneration statistics into single file and directory hierarchy versions
  - [x] Wrap StateSpaces
  - [ ] CLI Option to create data
  - [ ] Incorporate BPredicate/BElement classes more
- [ ] Training set manipulation
  - [ ] Split training set
  - [ ] upsample training set
  - [ ] downsample training set
  - [ ] shuffle training set
- [ ] Training set analysis
- [x] Migration from old pdump to new JSON

### Machine Learning algorithms

- [ ] Training of neural networks

### Other

- [ ] Enhance versioning of Backends

## Planned features

- [ ] enhanced documentation
  - [ ] documentation of JSON entries, (legacy) Predicate dumps, other formats
  - [ ] usage examples
    - [ ] Training data generation
    - [ ] Training/using neural networks
- [ ] RNN support
  - [x] set training set structure
    - [ ] set/implement appropriate RecordReader
  - [x] create RNNTrainingDataGenerator(s)
  - [ ] create RNN features
    - [x] raw predicate features
    - [ ] predicate AST features
- [ ] Data augmentation utilities
  - [ ] PCA: whitening
  - [ ] add type information to identifiers
    - [ ] hungarian notation: "x + y" -> "xInt + yInt"
    - [ ] joshua notation: "x + y" -> "i1 + i2"
- [ ] Enhanced analysis of feature sets
  - [ ] feature dimensionality reduction
    - [ ] PCA
    - [ ] RBM
    - [ ] by decision trees
  - [ ] t-SNE
- [ ] Decision trees
  - [ ] Random forests 
  - [ ] Deep Forest

# Future Work

- model check machines
    - generate 100 states per solver
        - give credit to fastest one
    - or: state space exploration over a fixed time
        - give credit to the one with the most states generated
